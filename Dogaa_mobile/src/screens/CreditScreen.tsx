import { Ionicons } from "@expo/vector-icons";
import React, { useState } from "react";
import { StyleSheet, Text, TouchableOpacity, View } from "react-native";
import { Card, IconCircle, Pill, Progress, Screen } from "../components/Layout";
import { c } from "../theme";
import { Route } from "../types";
import { useDogaaData } from "../context/DogaaDataContext";

export default function CreditScreen({
  navigate,
}: {
  navigate: (r: Route) => void;
}) {
  const [historyOpen, setHistoryOpen] = useState(false);
  const dogaa = useDogaaData();
  const activeLoans = dogaa.loans.filter(loan=>!['REPAID','DEFAULTED'].includes(loan.status));
  const outstanding = activeLoans.reduce((sum,loan)=>sum+Number(loan.outstanding||0),0);
  const currentLoans = activeLoans.map(loan=>({title:`Prêt DOGAA ${loan.id.slice(0,8)}`,amount:new Intl.NumberFormat('fr-FR').format(Number(loan.principal)),remaining:new Intl.NumberFormat('fr-FR').format(Number(loan.outstanding)),paid:loan.totalDue?Math.round(Number(loan.amountRepaid)/Number(loan.totalDue)*100):0,next:new Date(loan.dueAt).toLocaleDateString('fr-FR'),installment:`${new Intl.NumberFormat('fr-FR').format(Number(loan.outstanding))} FCFA`,icon:'cash-outline' as const}));
  const history = dogaa.loans.filter(loan=>['REPAID','DEFAULTED'].includes(loan.status)).map(loan=>({title:`Prêt DOGAA ${loan.id.slice(0,8)}`,amount:new Intl.NumberFormat('fr-FR').format(Number(loan.principal)),date:loan.settledAt?`Terminé le ${new Date(loan.settledAt).toLocaleDateString('fr-FR')}`:loan.status,icon:loan.status==='REPAID'?'checkmark-circle-outline' as const:'alert-circle-outline' as const}));
  return (
    <Screen route="credit" navigate={navigate}>
      <View style={s.head}>
        <View>
          <Text style={s.overline}>ESPACE FINANCEMENT</Text>
          <Text style={s.title}>Mes crédits</Text>
          <Text style={s.subtitle}>
            Suivez vos prêts et découvrez vos offres.
          </Text>
        </View>
        <View style={s.headIcon}>
          <Ionicons name="card-outline" size={23} color={c.yellow} />
        </View>
      </View>
      <View style={s.summary}>
        <View style={s.summaryItem}>
          <Text style={s.summaryLabel}>EN COURS</Text>
          <Text style={s.summaryValue}>{activeLoans.length}</Text>
        </View>
        <View style={s.summaryDivider} />
        <View style={s.summaryItem}>
          <Text style={s.summaryLabel}>CAPITAL RESTANT</Text>
          <Text style={s.summaryAmount}>
            {new Intl.NumberFormat('fr-FR').format(outstanding)} <Text style={s.fcfa}>FCFA</Text>
          </Text>
        </View>
      </View>
      <View style={s.section}>
        <Text style={s.sectionTitle}>Prêts en cours</Text>
        <Pill green>À jour</Pill>
      </View>
      {currentLoans.map((loan) => (
        <TouchableOpacity
          key={loan.title}
          onPress={() => navigate("loanDetail")}
          activeOpacity={0.8}
        >
          <Card style={s.loanCard}>
            <View style={s.loanTop}>
              <IconCircle name={loan.icon} />
              <View style={s.loanCopy}>
                <Text style={s.loanTitle}>{loan.title}</Text>
                <Text style={s.loanInitial}>
                  Montant initial : {loan.amount} FCFA
                </Text>
              </View>
              <Ionicons name="chevron-forward" size={20} color={c.primary} />
            </View>
            <View style={s.loanNumbers}>
              <View>
                <Text style={s.label}>RESTANT À PAYER</Text>
                <Text style={s.remaining}>
                  {loan.remaining} <Text style={s.small}>FCFA</Text>
                </Text>
              </View>
              <View style={s.alignRight}>
                <Text style={s.label}>PROGRESSION</Text>
                <Text style={s.percent}>{loan.paid}% remboursé</Text>
              </View>
            </View>
            <Progress value={loan.paid} color={c.green} />
            <View style={s.next}>
              <Ionicons name="calendar-outline" size={16} color={c.primary} />
              <View style={s.nextCopy}>
                <Text style={s.nextLabel}>Prochaine échéance</Text>
                <Text style={s.nextValue}>{loan.next}</Text>
              </View>
              <Text style={s.installment}>{loan.installment}</Text>
            </View>
            <Text style={s.tapHint}>
              Touchez la carte pour afficher tous les détails
            </Text>
          </Card>
        </TouchableOpacity>
      ))}
      {!dogaa.loading&&currentLoans.length===0&&<Card style={s.emptyCard}><Ionicons name="document-text-outline" size={30} color={c.muted}/><Text style={s.emptyTitle}>Aucun prêt en cours</Text><Text style={s.emptyText}>Vos futurs prêts apparaîtront ici.</Text></Card>}
      <View style={s.section}>
        <Text style={s.sectionTitle}>{dogaa.eligibility?.eligible?'Offre disponible':'Accès au crédit'}</Text>
        <Pill>Score {dogaa.eligibility?.score??0}/100</Pill>
      </View>
      <TouchableOpacity disabled={!dogaa.eligibility?.eligible} onPress={() => navigate("loan")} style={[s.offer,!dogaa.eligibility?.eligible&&{opacity:.72}]}>
        <View style={s.offerTop}>
          <View style={s.offerIcon}>
            <Ionicons name="flash-outline" size={24} color={c.primary} />
          </View>
          <View style={s.offerCopy}>
            <Text style={s.offerTitle}>{dogaa.eligibility?.eligible?'Prêt Express pré-approuvé':'Crédit non disponible'}</Text>
            <Text style={s.offerSub}>{dogaa.eligibility?.eligible?'Sans caution • Réponse immédiate':dogaa.eligibility?.blockers?.[0]||'Calcul de l’éligibilité indisponible'}</Text>
          </View>
        </View>
        <Text style={s.offerLabel}>JUSQU’À</Text>
        <Text style={s.offerAmount}>
          {new Intl.NumberFormat('fr-FR').format(Number(dogaa.eligibility?.maxLoanAmount||0))} <Text style={s.offerFcfa}>FCFA</Text>
        </Text>
        <View style={s.offerButton}>
          <Text style={s.offerButtonText}>{dogaa.eligibility?.eligible?'Simuler mon prêt':'Conditions non remplies'}</Text>
          <Ionicons name="arrow-forward" size={18} color={c.primary} />
        </View>
      </TouchableOpacity>
      <TouchableOpacity
        onPress={() => setHistoryOpen((v) => !v)}
        style={s.historyHead}
      >
        <View style={s.historyTitle}>
          <Ionicons name="time-outline" size={19} color={c.primary} />
          <Text style={s.sectionTitle}>Historique des prêts</Text>
        </View>
        <Ionicons
          name={historyOpen ? "chevron-up" : "chevron-down"}
          size={19}
          color={c.primary}
        />
      </TouchableOpacity>
      {historyOpen && (
        <Card style={s.historyCard}>
          {history.length===0&&<View style={s.emptyHistory}><Text style={s.emptyText}>Aucun ancien prêt.</Text></View>}
          {history.map((loan, index) => (
            <TouchableOpacity
              key={loan.title}
              onPress={() => navigate("loanDetail")}
              style={[s.historyRow, index > 0 && s.historyBorder]}
            >
              <Ionicons name={loan.icon} size={22} color={c.green} />
              <View style={s.historyCopy}>
                <Text style={s.historyName}>{loan.title}</Text>
                <Text style={s.historyDate}>{loan.date}</Text>
              </View>
              <Text style={s.historyAmount}>{loan.amount} F</Text>
              <Ionicons name="chevron-forward" size={16} color={c.muted} />
            </TouchableOpacity>
          ))}
        </Card>
      )}
    </Screen>
  );
}

const s = StyleSheet.create({
  head: {
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "center",
    gap: 12,
  },
  overline: { fontSize: 8, fontWeight: "800", color: c.greenDark },
  title: { fontSize: 25, fontWeight: "900", color: c.ink, marginTop: 2 },
  subtitle: { fontSize: 10, color: c.muted, marginTop: 3 },
  headIcon: {
    width: 49,
    height: 49,
    borderRadius: 15,
    backgroundColor: c.primary,
    alignItems: "center",
    justifyContent: "center",
  },
  summary: {
    backgroundColor: c.primary,
    borderRadius: 14,
    padding: 17,
    marginTop: 18,
    flexDirection: "row",
    alignItems: "center",
  },
  summaryItem: { flex: 1 },
  summaryDivider: {
    width: 1,
    height: 42,
    backgroundColor: "#FFFFFF22",
    marginHorizontal: 15,
  },
  summaryLabel: { fontSize: 8, fontWeight: "700", color: "#ADC6FF" },
  summaryValue: {
    fontSize: 25,
    fontWeight: "900",
    color: c.yellow,
    marginTop: 3,
  },
  summaryAmount: {
    fontSize: 20,
    fontWeight: "900",
    color: c.white,
    marginTop: 3,
  },
  fcfa: { fontSize: 9, color: c.yellow },
  section: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
    marginTop: 23,
    marginBottom: 11,
  },
  sectionTitle: { fontSize: 17, fontWeight: "800", color: c.ink },
  loanCard: { padding: 15 },
  loanTop: { flexDirection: "row", alignItems: "center", gap: 10 },
  loanCopy: { flex: 1, minWidth: 0 },
  loanTitle: { fontSize: 14, fontWeight: "800", color: c.primary },
  loanInitial: { fontSize: 9, color: c.muted, marginTop: 3 },
  loanNumbers: {
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "flex-end",
    marginTop: 17,
    marginBottom: 9,
  },
  label: { fontSize: 7, fontWeight: "700", color: c.muted },
  remaining: {
    fontSize: 21,
    fontWeight: "900",
    color: c.primary,
    marginTop: 3,
  },
  small: { fontSize: 9, color: c.yellowDark },
  alignRight: { alignItems: "flex-end" },
  percent: {
    fontSize: 10,
    fontWeight: "700",
    color: c.greenDark,
    marginTop: 4,
  },
  next: {
    flexDirection: "row",
    alignItems: "center",
    gap: 8,
    backgroundColor: c.pale,
    marginTop: 13,
    padding: 10,
    borderRadius: 9,
  },
  nextCopy: { flex: 1 },
  nextLabel: { fontSize: 8, color: c.muted },
  nextValue: { fontSize: 10, fontWeight: "700", color: c.ink, marginTop: 2 },
  installment: { fontSize: 11, fontWeight: "800", color: c.primary },
  tapHint: { fontSize: 8, color: c.muted, textAlign: "center", marginTop: 10 },
  offer: { borderRadius: 16, backgroundColor: c.yellow, padding: 17 },
  offerTop: { flexDirection: "row", alignItems: "center", gap: 10 },
  offerIcon: {
    width: 43,
    height: 43,
    borderRadius: 22,
    backgroundColor: "#FFFFFF99",
    alignItems: "center",
    justifyContent: "center",
  },
  offerCopy: { flex: 1 },
  offerTitle: { fontSize: 14, fontWeight: "900", color: c.primary },
  offerSub: { fontSize: 9, color: c.yellowDark, marginTop: 3 },
  offerLabel: {
    fontSize: 8,
    fontWeight: "800",
    color: c.yellowDark,
    marginTop: 16,
  },
  offerAmount: { fontSize: 27, fontWeight: "900", color: c.primary },
  offerFcfa: { fontSize: 13 },
  offerButton: {
    height: 42,
    borderRadius: 21,
    backgroundColor: c.white,
    marginTop: 13,
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "center",
    gap: 8,
  },
  offerButtonText: { fontSize: 12, fontWeight: "800", color: c.primary },
  historyHead: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
    marginTop: 24,
    marginBottom: 10,
  },
  historyTitle: { flexDirection: "row", alignItems: "center", gap: 7 },
  historyCard: { paddingVertical: 2 },
  historyRow: {
    minHeight: 65,
    flexDirection: "row",
    alignItems: "center",
    gap: 9,
  },
  historyBorder: { borderTopWidth: 1, borderTopColor: c.border },
  historyCopy: { flex: 1, minWidth: 0 },
  historyName: { fontSize: 12, fontWeight: "700", color: c.ink },
  historyDate: { fontSize: 8, color: c.muted, marginTop: 3 },
  historyAmount: { fontSize: 11, fontWeight: "800", color: c.primary },
  emptyCard: { alignItems: "center", paddingVertical: 25 },
  emptyTitle: { fontSize: 14, fontWeight: "800", color: c.ink, marginTop: 8 },
  emptyText: { fontSize: 10, color: c.muted, marginTop: 4, textAlign: "center" },
  emptyHistory: { paddingVertical: 20, alignItems: "center" },
});
