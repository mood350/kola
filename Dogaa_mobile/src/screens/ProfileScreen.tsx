import { Ionicons } from "@expo/vector-icons";
import React, { useState } from "react";
import { Alert, StyleSheet, Text, TouchableOpacity, View } from "react-native";
import { Card, IconCircle, Pill, Progress, Screen } from "../components/Layout";
import { c } from "../theme";
import { Route } from "../types";
import { useDogaaData } from "../context/DogaaDataContext";

const options = [
  {
    icon: "sparkles-outline" as const,
    title: "XP & niveau",
    sub: "7 800 XP • Niveau Élite",
    key: "xp",
  },
  {
    icon: "shield-checkmark-outline" as const,
    title: "Passer le KYC",
    sub: "Vérifiez votre identité et passez au TIER 3",
    key: "kyc",
  },
  {
    icon: "time-outline" as const,
    title: "Historique du compte",
    sub: "Connexions, activités et récompenses",
    key: "history",
  },
  {
    icon: "document-text-outline" as const,
    title: "Documents personnels",
    sub: "Pièce d’identité et justificatifs",
    key: "documents",
  },
  {
    icon: "help-circle-outline" as const,
    title: "FAQ et assistance",
    sub: "Réponses aux questions fréquentes",
    key: "faq",
  },
];

export default function ProfileScreen({
  navigate,
  onLogout,
}: {
  navigate: (r: Route) => void;
  onLogout: () => void;
}) {
  const [section, setSection] = useState<string | null>(null);
  const dogaa = useDogaaData();
  return (
    <Screen route="profile" navigate={navigate}>
      <View style={s.profileHead}>
        <View style={s.avatar}>
          <Ionicons name="person" size={34} color={c.white} />
        </View>
        <View style={s.identity}>
          <Text style={s.name}>{dogaa.user?`${dogaa.user.firstName} ${dogaa.user.lastName}`:'Chargement…'}</Text>
          <Text style={s.phone}>🇹🇬 {dogaa.user?.phone||'+228'}</Text>
          <View style={s.badges}>
            <Pill green>Compte vérifié</Pill>
            <Pill>{dogaa.user?.kycTier||'TIER 0'}</Pill>
          </View>
        </View>
        <TouchableOpacity style={s.edit}>
          <Ionicons name="create-outline" size={19} color={c.primary} />
        </TouchableOpacity>
      </View>
      <Card style={s.level}>
        <View style={s.levelTop}>
          <View>
            <Text style={s.overline}>NIVEAU DOGAA</Text>
            <Text style={s.levelName}>Élite</Text>
          </View>
          <View style={s.xp}>
            <Ionicons name="sparkles" size={17} color={c.yellowDark} />
            <Text style={s.xpValue}>7 800 XP</Text>
          </View>
        </View>
        <Progress value={78} color={c.yellow} />
        <View style={s.levelBottom}>
          <Text style={s.levelHint}>Progression vers TIER 3</Text>
          <Text style={s.levelHint}>2 200 XP restants</Text>
        </View>
      </Card>
      <Text style={s.title}>Mon profil</Text>
      <View style={s.menu}>
        {options.map((item) => (
          <TouchableOpacity
            key={item.key}
            onPress={() =>
              item.key === "kyc"
                ? navigate("kyc")
                : item.key === "history"
                  ? navigate("transactionHistory")
                  : item.key === "faq"
                    ? navigate("faq")
                  : setSection(section === item.key ? null : item.key)
            }
            style={s.option}
          >
            <IconCircle name={item.icon} />
            <View style={s.optionCopy}>
              <Text style={s.optionTitle}>{item.title}</Text>
              <Text style={s.optionSub}>{item.sub}</Text>
            </View>
            <Ionicons
              name={section === item.key ? "chevron-up" : "chevron-forward"}
              size={18}
              color={c.muted}
            />
            {section === item.key && (
              <View style={s.expanded}>
                <Expanded type={item.key} />
              </View>
            )}
          </TouchableOpacity>
        ))}
      </View>
      <TouchableOpacity onPress={() => navigate("credit")} style={s.creditLink}>
        <Ionicons name="card-outline" size={20} color={c.yellow} />
        <Text style={s.creditText}>Voir mes crédits et mes prêts</Text>
        <Ionicons name="arrow-forward" size={18} color={c.white} />
      </TouchableOpacity>
      <TouchableOpacity onPress={()=>Alert.alert("Déconnexion","Voulez-vous vraiment vous déconnecter ?",[{text:"Annuler",style:"cancel"},{text:"Se déconnecter",style:"destructive",onPress:onLogout}])} style={s.logout}>
        <Ionicons name="log-out-outline" size={20} color="#BA1A1A" />
        <Text style={s.logoutText}>Se déconnecter</Text>
      </TouchableOpacity>
    </Screen>
  );
}

function Expanded({ type }: { type: string }) {
  if (type === "kyc")
    return (
      <View style={s.detail}>
        <Detail icon="person-outline" text="Identité et téléphone vérifiés" />
        <Detail
          icon="location-outline"
          text="Justificatif de domicile requis pour TIER 3"
        />
      </View>
    );
  if (type === "history")
    return (
      <View style={s.detail}>
        <Detail icon="log-in-outline" text="Connexion aujourd’hui à 11:42" />
        <Detail icon="trophy-outline" text="+120 XP gagnés cette semaine" />
      </View>
    );
  if (type === "documents")
    return (
      <View style={s.detail}>
        <Detail
          icon="checkmark-circle-outline"
          text="Pièce d’identité validée"
        />
        <Detail
          icon="add-circle-outline"
          text="Ajouter un justificatif de domicile"
        />
      </View>
    );
  return (
    <View style={s.detail}>
      <Detail icon="wallet-outline" text="Dépôts réguliers : 95/100" />
      <Detail icon="business-outline" text="Discipline Bankivi : 88/100" />
      <Detail icon="storefront-outline" text="Transactions : 72/100" />
    </View>
  );
}
function Detail({
  icon,
  text,
}: {
  icon: React.ComponentProps<typeof Ionicons>["name"];
  text: string;
}) {
  return (
    <View style={s.detailRow}>
      <Ionicons name={icon} size={16} color={c.greenDark} />
      <Text style={s.detailText}>{text}</Text>
    </View>
  );
}
const s = StyleSheet.create({
  profileHead: { flexDirection: "row", alignItems: "center", gap: 12 },
  avatar: {
    width: 64,
    height: 64,
    borderRadius: 32,
    backgroundColor: c.primary,
    alignItems: "center",
    justifyContent: "center",
  },
  identity: { flex: 1, minWidth: 0 },
  name: { fontSize: 20, fontWeight: "800", color: c.ink },
  phone: { fontSize: 10, color: c.muted, marginVertical: 4 },
  badges: { flexDirection: "row", gap: 5 },
  edit: {
    width: 38,
    height: 38,
    borderRadius: 19,
    backgroundColor: c.pale2,
    alignItems: "center",
    justifyContent: "center",
  },
  level: { marginTop: 18, backgroundColor: c.primary },
  levelTop: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
    marginBottom: 13,
  },
  overline: { fontSize: 8, fontWeight: "800", color: "#ADC6FF" },
  levelName: { fontSize: 22, fontWeight: "900", color: c.white, marginTop: 2 },
  xp: {
    flexDirection: "row",
    alignItems: "center",
    gap: 5,
    backgroundColor: c.yellow,
    borderRadius: 15,
    paddingHorizontal: 10,
    paddingVertical: 6,
  },
  xpValue: { fontSize: 11, fontWeight: "900", color: c.primary },
  levelBottom: {
    flexDirection: "row",
    justifyContent: "space-between",
    marginTop: 8,
  },
  levelHint: { fontSize: 8, color: "#D8E2FF" },
  title: {
    fontSize: 19,
    fontWeight: "800",
    color: c.ink,
    marginTop: 24,
    marginBottom: 11,
  },
  menu: { gap: 8 },
  option: {
    borderRadius: 14,
    backgroundColor: c.white,
    borderWidth: 1,
    borderColor: c.border,
    padding: 12,
    flexDirection: "row",
    alignItems: "center",
    gap: 10,
    flexWrap: "wrap",
  },
  optionCopy: { flex: 1, minWidth: 0 },
  optionTitle: { fontSize: 13, fontWeight: "700", color: c.ink },
  optionSub: { fontSize: 9, lineHeight: 13, color: c.muted, marginTop: 2 },
  expanded: { width: "100%", paddingLeft: 53 },
  detail: {
    borderTopWidth: 1,
    borderTopColor: c.border,
    paddingTop: 9,
    gap: 8,
  },
  detailRow: { flexDirection: "row", alignItems: "center", gap: 7 },
  detailText: { fontSize: 9, lineHeight: 13, color: c.muted, flex: 1 },
  creditLink: {
    minHeight: 54,
    borderRadius: 27,
    backgroundColor: c.primary,
    marginTop: 18,
    paddingHorizontal: 17,
    flexDirection: "row",
    alignItems: "center",
    gap: 9,
  },
  creditText: { fontSize: 12, fontWeight: "800", color: c.white, flex: 1 },
  logout: {height:52,borderRadius:26,borderWidth:1,borderColor:"#F0B8B8",marginTop:12,flexDirection:"row",alignItems:"center",justifyContent:"center",gap:8,backgroundColor:"#FFF8F8"},
  logoutText: {fontSize:12,fontWeight:"800",color:"#BA1A1A"},
});
