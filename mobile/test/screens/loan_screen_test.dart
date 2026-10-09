import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/providers/kola_data_provider.dart';
import 'package:mobile/screens/loan_screen.dart';
import 'package:provider/provider.dart';

void main() {
  testWidgets('le montant du prêt se saisit au clavier', (tester) async {
    await tester.pumpWidget(
      MaterialApp(
        home: Scaffold(
          body: ChangeNotifierProvider(
            create: (_) => KolaDataProvider(),
            child: LoanScreen(onNavigate: (_) {}),
          ),
        ),
      ),
    );

    final field = find.byType(TextField);
    expect(field, findsOneWidget);

    await tester.tap(field);
    await tester.enterText(field, '45000');
    await tester.pump();

    expect(find.text('45000'), findsOneWidget);
    // Sans éligibilité chargée le plafond vaut 0 : un montant tapé au-dessus
    // n'est pas ramené en silence, il est signalé.
    expect(find.textContaining('Votre plafond est de'), findsOneWidget);
  });
}
