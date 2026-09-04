import { Ionicons } from '@expo/vector-icons';
import React, { useState } from 'react';
import { Alert, KeyboardAvoidingView, Modal, Platform, ScrollView, StyleSheet, Text, TextInput, TouchableOpacity, View } from 'react-native';
import { Card, IconCircle, Pill, Progress, Screen, SectionTitle } from '../components/Layout';
import { useDogaaData } from '../context/DogaaDataContext';
import { vaultApi } from '../services/api';
import { c } from '../theme';
import { Route } from '../types';

const money=(value:number)=>new Intl.NumberFormat('fr-FR').format(value);

export default function VaultsScreen({navigate}:{navigate:(r:Route)=>void}){
  const dogaa=useDogaaData();
  const [open,setOpen]=useState(false),[name,setName]=useState(''),[description,setDescription]=useState(''),[target,setTarget]=useState(''),[date,setDate]=useState(''),[saving,setSaving]=useState(false);
  const total=dogaa.vaults.reduce((sum,v)=>sum+Number(v.balance),0);
  const close=()=>{setOpen(false);setName('');setDescription('');setTarget('');setDate('');};
  const create=async()=>{
    const amount=Number(target.replace(/\D/g,''));
    if(name.trim().length<2){Alert.alert('Nom requis','Donnez un nom à votre coffre.');return;}
    if(target&&amount<=0){Alert.alert('Objectif invalide','Saisissez un montant supérieur à zéro.');return;}
    if(date&&!/^\d{4}-\d{2}-\d{2}$/.test(date)){Alert.alert('Date invalide','Utilisez le format AAAA-MM-JJ.');return;}
    setSaving(true);
    try{await vaultApi.create({name:name.trim(),description:description.trim()||undefined,targetAmount:amount||undefined,targetDate:date||undefined});await dogaa.refresh();close();Alert.alert('Coffre créé','Votre nouveau coffre est prêt.');}
    catch(error){Alert.alert('Création impossible',error instanceof Error?error.message:'Erreur serveur.');}
    finally{setSaving(false);}
  };
  return <Screen route="vaults" navigate={navigate}>
    <View style={s.hero}><View style={s.heroHead}><View style={s.heroLabel}><Ionicons name="lock-closed-outline" color={c.yellow} size={18}/><Text style={s.heroOver}>ÉPARGNE TOTALE{`\n`}SÉCURISÉE</Text></View><Pill green>{dogaa.vaults.length} coffre{dogaa.vaults.length>1?'s':''}</Pill></View><Text style={s.heroAmount}>{money(total)} <Text style={s.fcfa}>FCFA</Text></Text><Text style={s.heroCopy}>Fonds protégés pour vos projets prioritaires et votre solvabilité.</Text></View>
    <View style={s.coach}><IconCircle name="sparkles-outline" bg="#FFF4C6" color={c.yellowDark}/><View style={{flex:1}}><Text style={s.coachTitle}>Discipline d’épargne</Text><Text style={s.coachText}>Chaque versement régulier renforce votre profil financier DOGAA.</Text></View></View>
    <TouchableOpacity onPress={()=>setOpen(true)} style={s.createButton}><Ionicons name="add-circle-outline" size={21} color={c.primary}/><Text style={s.createText}>Créer un nouveau coffre</Text></TouchableOpacity>
    <SectionTitle title={`Vos Coffres (${dogaa.vaults.length})`} action="TOGO"/>
    {!dogaa.loading&&dogaa.vaults.length===0&&<Card style={s.empty}><Ionicons name="lock-open-outline" size={34} color={c.muted}/><Text style={s.emptyTitle}>Aucun coffre</Text><Text style={s.emptyText}>Créez votre premier objectif d’épargne.</Text></Card>}
    {dogaa.vaults.map(v=><Card key={v.id} style={s.vault}><View style={s.row}><IconCircle name="lock-closed-outline"/><View style={s.info}><Text style={s.title}>{v.name}</Text><Text style={s.sub}>{v.description||'Coffre DOGAA'}</Text></View><Pill>{v.status}</Pill></View><View style={s.cumul}><Text style={s.cumulText}>Cumul : <Text style={s.bold}>{money(Number(v.balance))} FCFA</Text></Text><Text style={s.pct}>{v.progressPercent||0}%</Text></View><Progress value={v.progressPercent||0}/><View style={s.labels}><Text style={s.labelSmall}>0 FCFA</Text><Text style={s.labelSmall}>Objectif {money(Number(v.targetAmount||0))} FCFA</Text></View>{v.targetDate&&<View style={s.plan}><Ionicons name="calendar-outline" size={16} color={c.green}/><Text style={s.planText}>Objectif au {new Date(v.targetDate).toLocaleDateString('fr-FR')}</Text></View>}</Card>)}
    <View style={s.rules}><View style={s.ruleTitleRow}><Ionicons name="settings-outline" size={19} color={c.primary}/><Text style={s.rulesTitle}>Règles et impact</Text></View><Rule icon="lock-closed-outline" title="Verrouillage strict" text="Les fonds restent protégés dans le coffre."/><Rule icon="trophy-outline" title="Discipline récompensée" text="L’épargne régulière améliore votre profil financier."/></View>
    <Modal transparent visible={open} animationType="slide" onRequestClose={close}><KeyboardAvoidingView style={s.overlay} behavior={Platform.OS==='ios'?'padding':undefined}><View style={s.sheet}><View style={s.sheetHead}><View><Text style={s.sheetTitle}>Nouveau coffre</Text><Text style={s.sheetSub}>Définissez votre objectif d’épargne.</Text></View><TouchableOpacity onPress={close}><Ionicons name="close" size={24} color={c.ink}/></TouchableOpacity></View><ScrollView keyboardShouldPersistTaps="handled">
      <Label text="NOM DU COFFRE"/><TextInput value={name} onChangeText={setName} placeholder="Ex. Études, urgence, projet" placeholderTextColor={c.muted} style={s.input}/>
      <Label text="DESCRIPTION (FACULTATIVE)"/><TextInput value={description} onChangeText={setDescription} placeholder="À quoi servira cette épargne ?" placeholderTextColor={c.muted} style={s.input}/>
      <Label text="OBJECTIF EN FCFA (FACULTATIF)"/><TextInput value={target} onChangeText={value=>setTarget(value.replace(/\D/g,''))} keyboardType="number-pad" placeholder="500000" placeholderTextColor={c.muted} style={s.input}/>
      <Label text="DATE CIBLE (FACULTATIVE)"/><TextInput value={date} onChangeText={setDate} placeholder="AAAA-MM-JJ" placeholderTextColor={c.muted} maxLength={10} style={s.input}/>
      <TouchableOpacity disabled={saving} onPress={create} style={[s.createButton,saving&&{opacity:.6}]}><Ionicons name={saving?'hourglass-outline':'checkmark-circle-outline'} size={21} color={c.primary}/><Text style={s.createText}>{saving?'Création…':'Créer le coffre'}</Text></TouchableOpacity>
    </ScrollView></View></KeyboardAvoidingView></Modal>
  </Screen>;
}

function Label({text}:{text:string}){return <Text style={s.formLabel}>{text}</Text>}
function Rule({icon,title,text}:{icon:React.ComponentProps<typeof Ionicons>['name'];title:string;text:string}){return <View style={s.rule}><Ionicons name={icon} size={18} color={c.greenDark}/><Text style={s.ruleText}><Text style={s.bold}>{title} : </Text>{text}</Text></View>}

const s=StyleSheet.create({hero:{backgroundColor:c.primary,borderRadius:15,padding:20},heroHead:{flexDirection:'row',justifyContent:'space-between'},heroLabel:{flexDirection:'row',gap:8},heroOver:{fontSize:11,lineHeight:16,fontWeight:'800',color:'#ADC6FF'},heroAmount:{fontSize:28,fontWeight:'900',color:c.yellow,marginTop:14},fcfa:{fontSize:16,color:c.white},heroCopy:{fontSize:11,lineHeight:17,color:'#E3E7F5',marginTop:12},coach:{marginVertical:24,borderRadius:15,backgroundColor:c.pale,padding:19,flexDirection:'row',gap:12},coachTitle:{fontSize:16,fontWeight:'700',color:c.primary},coachText:{fontSize:11,lineHeight:17,color:c.muted,marginTop:4},createButton:{height:52,borderRadius:26,backgroundColor:c.yellow,flexDirection:'row',alignItems:'center',justifyContent:'center',gap:8},createText:{fontSize:13,fontWeight:'800',color:c.primary},empty:{alignItems:'center',paddingVertical:25},emptyTitle:{fontSize:14,fontWeight:'800',color:c.ink,marginTop:8},emptyText:{fontSize:10,color:c.muted,marginTop:4},vault:{marginBottom:14},row:{flexDirection:'row',alignItems:'center',gap:10},info:{flex:1},title:{fontSize:16,fontWeight:'700',color:c.primary},sub:{fontSize:10,fontWeight:'600',color:c.ink,marginTop:3},cumul:{flexDirection:'row',justifyContent:'space-between',marginTop:17,marginBottom:9},cumulText:{fontSize:11,color:c.muted},bold:{fontWeight:'800',color:c.ink},pct:{fontSize:12,fontWeight:'700',color:c.primary},labels:{flexDirection:'row',justifyContent:'space-between',marginTop:7},labelSmall:{fontSize:9,color:c.muted},plan:{height:39,borderRadius:9,backgroundColor:c.pale,flexDirection:'row',alignItems:'center',paddingHorizontal:12,marginTop:13,gap:8},planText:{fontSize:10,color:c.muted,flex:1},rules:{padding:20,borderRadius:15,backgroundColor:c.pale,marginTop:10,gap:12},ruleTitleRow:{flexDirection:'row',alignItems:'center',gap:8},rulesTitle:{fontSize:16,fontWeight:'700',color:c.primary},rule:{flexDirection:'row',alignItems:'flex-start',gap:9},ruleText:{fontSize:11,lineHeight:17,color:c.muted,flex:1},overlay:{flex:1,backgroundColor:'#00235366',justifyContent:'flex-end'},sheet:{maxHeight:'88%',backgroundColor:c.white,borderTopLeftRadius:27,borderTopRightRadius:27,padding:22,paddingBottom:35},sheetHead:{flexDirection:'row',justifyContent:'space-between'},sheetTitle:{fontSize:20,fontWeight:'900',color:c.primary},sheetSub:{fontSize:10,color:c.muted,marginTop:4},formLabel:{fontSize:9,fontWeight:'800',color:c.muted,marginTop:17,marginBottom:6},input:{height:48,borderRadius:11,borderWidth:1,borderColor:c.border,paddingHorizontal:12,color:c.ink,backgroundColor:c.white,marginBottom:2}});
