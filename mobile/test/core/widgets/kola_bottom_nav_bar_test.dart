import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/core/widgets/kola_bottom_nav_bar.dart';

/// Les debordements de layout ne se voient ni a l'analyse statique ni dans un
/// test de provider : ils ne surgissent qu'au rendu. D'ou ces tests, qui
/// couvrent le cas signale par l'emulateur (« A RenderFlex overflowed by 5.0
/// pixels ») et sa version animee.
void main() {
  Widget barre(int index) => MaterialApp(
    home: Scaffold(
      bottomNavigationBar: KolaBottomNavBar(currentIndex: index, onTap: (_) {}),
    ),
  );

  testWidgets('aucun debordement, quel que soit l onglet actif', (
    tester,
  ) async {
    for (var index = 0; index < 4; index++) {
      await tester.pumpWidget(barre(index));
      await tester.pumpAndSettle();

      // Le debordement ne se produisait que sur l'onglet actif : c'est lui qui
      // affiche la pastille sous l'icone, et elle ne rentrait pas.
      expect(tester.takeException(), isNull, reason: 'onglet actif $index');
    }
  });

  testWidgets('aucun debordement pendant la transition', (tester) async {
    await tester.pumpWidget(barre(0));
    await tester.pumpAndSettle();

    await tester.pumpWidget(barre(1));
    // On s'arrete au milieu de l'animation de 200 ms : c'est la que la
    // pastille avait une taille intermediaire, et que la Column changeait de
    // hauteur a chaque frame.
    await tester.pump(const Duration(milliseconds: 100));
    expect(tester.takeException(), isNull);

    await tester.pumpAndSettle();
    expect(tester.takeException(), isNull);
  });
}
