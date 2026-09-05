import { Ionicons } from '@expo/vector-icons';
import React, { useMemo, useState } from 'react';
import { StyleSheet, Text, TextInput, TouchableOpacity, View } from 'react-native';
import { Screen } from '../components/Layout';
import { c } from '../theme';
import { Route } from '../types';

type Faq={id:string;category:string;question:string;answer:string};
const faqs:Faq[]=[
  {id:'account',category:'Compte',question:'Comment créer et sécuriser mon compte DOGAA ?',answer:'Inscrivez-vous avec un numéro togolais, confirmez le code OTP puis choisissez un code PIN à 4 chiffres. Ne communiquez jamais votre OTP ou votre PIN.'},
  {id:'pin',category:'Compte',question:'Que faire si j’oublie mon code PIN ?',answer:'Depuis la page de connexion, utilisez la récupération du compte. Un code de vérification sera envoyé sur votre numéro enregistré.'},
  {id:'recharge',category:'Portefeuille',question:'Comment recharger mon portefeuille ?',answer:'Sur l’accueil, appuyez sur « Recharger », choisissez Mixx by Yas, Moov Money ou carte bancaire, puis confirmez le montant et le compte togolais.'},
  {id:'p2p',category:'Transferts',question:'Comment envoyer de l’argent à une personne ?',answer:'Appuyez sur « Envoyer P2P », scannez le QR DOGAA du bénéficiaire ou saisissez son numéro au format togolais, puis vérifiez les informations avant de confirmer.'},
  {id:'pending',category:'Transferts',question:'Pourquoi mon transfert est-il en attente ?',answer:'Une opération peut rester en attente pendant la confirmation de l’opérateur. Consultez l’historique avant de recommencer afin d’éviter un double envoi.'},
  {id:'vault',category:'Coffres',question:'À quoi sert un coffre DOGAA ?',answer:'Un coffre vous permet de mettre de l’argent de côté pour un objectif. Vous pouvez définir un montant cible et une date, puis suivre votre progression.'},
  {id:'schedule',category:'Planification',question:'Comment programmer un envoi ?',answer:'Ouvrez « Planifié », choisissez une personne ou un coffre, saisissez le montant, la date et l’heure du Togo, puis confirmez la programmation.'},
  {id:'credit',category:'Crédit',question:'Comment mon éligibilité au crédit est-elle calculée ?',answer:'Elle dépend notamment de votre vérification KYC, de votre activité, de votre discipline Bankivi et du remboursement de vos précédents crédits.'},
  {id:'security',category:'Sécurité',question:'DOGAA me demandera-t-il mon OTP ou mon PIN ?',answer:'Non. Aucun agent DOGAA ne doit vous demander votre code OTP ou votre PIN. Refusez et signalez immédiatement toute demande de ce type.'},
];

export default function FaqScreen({navigate}:{navigate:(route:Route)=>void}){
  const [query,setQuery]=useState(''),[open,setOpen]=useState<string|null>(null);
  const results=useMemo(()=>{const value=query.trim().toLocaleLowerCase('fr');return value?faqs.filter(item=>`${item.category} ${item.question} ${item.answer}`.toLocaleLowerCase('fr').includes(value)):faqs;},[query]);
  const groups=useMemo(()=>[...new Set(results.map(item=>item.category))].map(category=>({category,items:results.filter(item=>item.category===category)})),[results]);
  return <Screen route="faq" navigate={navigate}>
    <TouchableOpacity onPress={()=>navigate('profile')} style={s.back}><Ionicons name="arrow-back" size={19} color={c.primary}/><Text style={s.backText}>Mon profil</Text></TouchableOpacity>
    <View style={s.head}><View style={s.headCopy}><Text style={s.overline}>CENTRE D’AIDE</Text><Text style={s.title}>Comment pouvons-nous vous aider ?</Text><Text style={s.subtitle}>Retrouvez rapidement les réponses sur DOGAA.</Text></View><View style={s.headIcon}><Ionicons name="help-circle-outline" size={28} color={c.yellow}/></View></View>
    <View style={s.search}><Ionicons name="search-outline" size={19} color={c.muted}/><TextInput value={query} onChangeText={setQuery} placeholder="Rechercher une question" placeholderTextColor={c.muted} style={s.searchInput}/>{query.length>0&&<TouchableOpacity onPress={()=>setQuery('')}><Ionicons name="close-circle" size={18} color={c.muted}/></TouchableOpacity>}</View>
    {groups.map(group=><View key={group.category}><Text style={s.category}>{group.category.toUpperCase()}</Text><View style={s.list}>{group.items.map(item=>{const expanded=open===item.id;return <TouchableOpacity key={item.id} activeOpacity={.75} onPress={()=>setOpen(expanded?null:item.id)} style={[s.item,expanded&&s.itemOpen]}><View style={s.questionRow}><Text style={s.question}>{item.question}</Text><Ionicons name={expanded?'remove':'add'} size={20} color={c.primary}/></View>{expanded&&<Text style={s.answer}>{item.answer}</Text>}</TouchableOpacity>})}</View></View>)}
    {results.length===0&&<View style={s.empty}><Ionicons name="search-outline" size={36} color={c.muted}/><Text style={s.emptyTitle}>Aucune réponse trouvée</Text><Text style={s.emptyText}>Essayez avec un autre mot-clé.</Text></View>}
    <View style={s.security}><Ionicons name="shield-checkmark-outline" size={21} color={c.greenDark}/><View style={s.securityCopy}><Text style={s.securityTitle}>Conseil de sécurité</Text><Text style={s.securityText}>Ne partagez jamais votre code OTP ou votre PIN.</Text></View></View>
  </Screen>;
}

const s=StyleSheet.create({back:{height:38,alignSelf:'flex-start',flexDirection:'row',alignItems:'center',gap:7},backText:{fontSize:11,fontWeight:'700',color:c.primary},head:{flexDirection:'row',alignItems:'center',gap:14,marginTop:7},headCopy:{flex:1},overline:{fontSize:8,fontWeight:'900',color:c.greenDark},title:{fontSize:23,lineHeight:29,fontWeight:'900',color:c.ink,marginTop:3},subtitle:{fontSize:10,color:c.muted,marginTop:4},headIcon:{width:54,height:54,borderRadius:17,backgroundColor:c.primary,alignItems:'center',justifyContent:'center'},search:{height:49,borderRadius:14,borderWidth:1,borderColor:c.border,backgroundColor:c.white,flexDirection:'row',alignItems:'center',paddingHorizontal:13,gap:9,marginTop:21},searchInput:{flex:1,fontSize:12,color:c.ink},category:{fontSize:8,fontWeight:'900',color:c.muted,marginTop:20,marginBottom:8},list:{gap:8},item:{borderRadius:14,borderWidth:1,borderColor:c.border,backgroundColor:c.white,padding:14},itemOpen:{borderColor:'#B5C1F8',backgroundColor:c.pale},questionRow:{flexDirection:'row',alignItems:'center',gap:10},question:{flex:1,fontSize:12,lineHeight:17,fontWeight:'800',color:c.ink},answer:{fontSize:10,lineHeight:17,color:c.muted,borderTopWidth:1,borderTopColor:c.border,paddingTop:11,marginTop:11},empty:{alignItems:'center',paddingVertical:42},emptyTitle:{fontSize:14,fontWeight:'800',color:c.ink,marginTop:9},emptyText:{fontSize:10,color:c.muted,marginTop:4},security:{borderRadius:14,backgroundColor:'#DDFBED',padding:15,flexDirection:'row',alignItems:'center',gap:10,marginTop:22},securityCopy:{flex:1},securityTitle:{fontSize:11,fontWeight:'800',color:c.greenDark},securityText:{fontSize:9,color:c.muted,marginTop:3}});
