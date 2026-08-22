import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/core/widgets/kola_card.dart';

void main() {
  testWidgets('un ListTile dans une KolaCard peut peindre son encre', (
    tester,
  ) async {
    await tester.pumpWidget(
      MaterialApp(
        home: Scaffold(
          body: KolaCard(
            child: ListTile(title: const Text('Detail'), onTap: () {}),
          ),
        ),
      ),
    );

    // Tant que la carte etait un Container decore, Flutter levait ici
    // « ListTile background color or ink splashes may be invisible » : le
    // Material le plus proche etait celui du Scaffold, derriere le fond blanc
    // opaque de la carte.
    expect(tester.takeException(), isNull);
  });

  testWidgets('la carte fournit un Material a ses descendants', (tester) async {
    await tester.pumpWidget(
      const MaterialApp(
        home: Scaffold(body: KolaCard(child: Text('x'))),
      ),
    );

    // Un seul Material entre le texte et le Scaffold suffit a rendre l'encre
    // visible : c'est celui de la carte.
    final materialDeLaCarte = tester.widget<Material>(
      find.ancestor(of: find.text('x'), matching: find.byType(Material)).first,
    );
    expect(materialDeLaCarte.shape, isA<RoundedRectangleBorder>());
    expect(materialDeLaCarte.clipBehavior, Clip.antiAlias);
  });

  testWidgets('une carte tappable reste tappable', (tester) async {
    var taps = 0;
    await tester.pumpWidget(
      MaterialApp(
        home: Scaffold(
          body: KolaCard(onTap: () => taps++, child: const Text('x')),
        ),
      ),
    );

    await tester.tap(find.text('x'));
    expect(taps, 1);
  });
}
