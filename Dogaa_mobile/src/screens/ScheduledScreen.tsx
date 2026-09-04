import AsyncStorage from '@react-native-async-storage/async-storage';
import { Ionicons } from '@expo/vector-icons';
import * as Contacts from 'expo-contacts';
import React, { useEffect, useMemo, useState } from 'react';
import { Alert, KeyboardAvoidingView, Modal, Platform, ScrollView, StyleSheet, Text, TextInput, TouchableOpacity, View } from 'react-native';
import { Card, IconCircle, Pill, PrimaryButton, Screen } from '../components/Layout';
import { c } from '../theme';
import { Route } from '../types';

type Destination = 'person' | 'vault';
type ScheduledTransfer = { id:string; destination:Destination; recipient:string; phone?:string; vaultId?:string; amount:number; scheduledAt:string; status:'active'|'paused' };
const STORAGE_KEY='dogaa.scheduled-transfers.v1';
const vaults=[{id:'truck',name:'Réparation camion'},{id:'school',name:'Scolarité des enfants'},{id:'emergency',name:"Fonds d'urgence stock"}];
const examples:ScheduledTransfer[]=[
  {id:'demo-rent',destination:'person',recipient:'SCI Agence Centrale',phone:'Marchand N° 8842',amount:75000,scheduledAt:'2026-10-05T09:00:00',status:'active'},
  {id:'demo-vault',destination:'vault',recipient:'Réparation camion',vaultId:'truck',amount:25000,scheduledAt:'2026-10-15T08:00:00',status:'active'},
];
const money=(value:number)=>new Intl.NumberFormat('fr-FR').format(value);
const dateLabel=(iso:string)=>new Intl.DateTimeFormat('fr-FR',{day:'numeric',month:'long',year:'numeric',hour:'2-digit',minute:'2-digit'}).format(new Date(iso));
const localDate=(date:Date)=>`${date.getFullYear()}-${String(date.getMonth()+1).padStart(2,'0')}-${String(date.getDate()).padStart(2,'0')}`;
const localTime=(date:Date)=>`${String(date.getHours()).padStart(2,'0')}:${String(date.getMinutes()).padStart(2,'0')}`;

export default function ScheduledScreen({navigate}:{navigate:(r:Route)=>void}){
  const initial=new Date(Date.now()+86400000);
  const [tab,setTab]=useState(0),[formOpen,setFormOpen]=useState(false),[contactsOpen,setContactsOpen]=useState(false),[loaded,setLoaded]=useState(false);
  const [transfers,setTransfers]=useState<ScheduledTransfer[]>(examples);
  const [destination,setDestination]=useState<Destination>('person'),[recipient,setRecipient]=useState(''),[phone,setPhone]=useState(''),[vaultId,setVaultId]=useState(vaults[0].id),[amount,setAmount]=useState('');
  const [date,setDate]=useState(localDate(initial)),[time,setTime]=useState(localTime(initial));
  const [contacts,setContacts]=useState<{id:string;name:string;phone:string}[]>([]),[query,setQuery]=useState('');

  useEffect(()=>{AsyncStorage.getItem(STORAGE_KEY).then(raw=>{if(raw)setTransfers(JSON.parse(raw));}).catch(()=>{}).finally(()=>setLoaded(true));},[]);
  useEffect(()=>{if(loaded)AsyncStorage.setItem(STORAGE_KEY,JSON.stringify(transfers)).catch(()=>{});},[transfers,loaded]);
  const visible=useMemo(()=>transfers.filter(x=>tab===0?x.status==='active':tab===2?x.status==='paused':false),[transfers,tab]);
  const filteredContacts=useMemo(()=>contacts.filter(x=>`${x.name} ${x.phone}`.toLowerCase().includes(query.toLowerCase())),[contacts,query]);
  const resetForm=()=>{const next=new Date(Date.now()+86400000);setDestination('person');setRecipient('');setPhone('');setVaultId(vaults[0].id);setAmount('');setDate(localDate(next));setTime(localTime(next));};
  const closeForm=()=>{setFormOpen(false);resetForm();};
  const openContacts=async()=>{try{const permission=await Contacts.requestPermissionsAsync();if(permission.status!=='granted'){Alert.alert('Accès aux contacts refusé','Vous pouvez toujours saisir le numéro manuellement.');return;}const result=await Contacts.getContactsAsync({fields:[Contacts.Fields.PhoneNumbers],sort:Contacts.SortTypes.FirstName});setContacts(result.data.flatMap(contact=>{const number=contact.phoneNumbers?.[0]?.number;return number?[{id:contact.id,name:contact.name||'Sans nom',phone:number}]:[];}));setContactsOpen(true);}catch{Alert.alert('Contacts indisponibles','Saisissez le numéro manuellement.');}};
  const submit=()=>{
    const numericAmount=Number(amount.replace(/[^0-9]/g,''));
    const scheduled=new Date(`${date}T${time}:00`);
    if(destination==='person'&&(!recipient.trim()||phone.replace(/\D/g,'').length<8)){Alert.alert('Bénéficiaire incomplet','Ajoutez un nom et un numéro de téléphone valide.');return;}
    if(!numericAmount){Alert.alert('Montant invalide','Saisissez un montant supérieur à 0 FCFA.');return;}
    if(!/^\d{4}-\d{2}-\d{2}$/.test(date)||!/^([01]\d|2[0-3]):[0-5]\d$/.test(time)||Number.isNaN(scheduled.getTime())){Alert.alert('Date invalide','Utilisez AAAA-MM-JJ et HH:MM.');return;}
    if(scheduled.getTime()<=Date.now()){Alert.alert('Date passée','Choisissez une date et une heure futures.');return;}
    const vault=vaults.find(x=>x.id===vaultId)!;
    const transfer:ScheduledTransfer={id:`scheduled-${Date.now()}`,destination,recipient:destination==='vault'?vault.name:recipient.trim(),phone:destination==='person'?phone.trim():undefined,vaultId:destination==='vault'?vaultId:undefined,amount:numericAmount,scheduledAt:scheduled.toISOString(),status:'active'};
    setTransfers(current=>[transfer,...current]);closeForm();
    Alert.alert('Envoi programmé',`${money(numericAmount)} FCFA seront envoyés le ${dateLabel(transfer.scheduledAt)}.`);
  };
  const toggle=(item:ScheduledTransfer)=>setTransfers(current=>current.map(x=>x.id===item.id?{...x,status:x.status==='active'?'paused':'active'}:x));

  return <Screen route="scheduled" navigate={navigate}>
    <View style={s.titleRow}><Text style={s.title}>Envois programmés</Text><Pill green>● Automatisés</Pill></View>
    <View style={s.scheduler}><Ionicons name="shield-checkmark-outline" size={15} color={c.green}/><Text style={s.schedulerText}>Ordres enregistrés et contrôlables à tout moment</Text></View>
    <View style={s.summary}><Stat value={transfers.filter(x=>x.status==='active').length.toString()} label="ACTIFS" color={c.yellow}/><Stat value={transfers.filter(x=>x.status==='paused').length.toString()} label="EN PAUSE"/><Stat value="J-1" label="RAPPEL" color={c.mint}/></View>
    <PrimaryButton onPress={()=>setFormOpen(true)}>⊕  Programmer un envoi</PrimaryButton>
    <View style={s.tabs}>{[`À venir (${transfers.filter(x=>x.status==='active').length})`,'Historique',`En pause (${transfers.filter(x=>x.status==='paused').length})`].map((x,i)=><TouchableOpacity onPress={()=>setTab(i)} key={x} style={[s.tab,tab===i&&s.tabActive]}><Text style={s.tabText}>{x}</Text></TouchableOpacity>)}</View>
    {tab===1?<Empty icon="receipt-outline" text="Aucun envoi exécuté"/>:visible.length===0?<Empty icon="calendar-outline" text={tab===2?'Aucun envoi en pause':'Aucun envoi programmé'}/>:visible.map(item=><Card key={item.id} style={s.schedule}>
      <View style={s.itemHead}><IconCircle name={item.destination==='vault'?'lock-closed-outline':'person-outline'}/><View style={s.info}><Text style={s.itemTitle}>{item.recipient}</Text><Pill>{item.destination==='vault'?'Coffre':'Personne'}</Pill><Text style={s.sub}>{item.destination==='vault'?'Épargne sécurisée':item.phone}</Text></View><TouchableOpacity onPress={()=>toggle(item)} style={s.pause}><Ionicons name={item.status==='active'?'pause':'play'} size={18} color={c.primary}/></TouchableOpacity></View>
      <Text style={s.amount}>{money(item.amount)}<Text style={s.fcfa}> FCFA</Text></Text><View style={s.status}><Ionicons name="calendar-outline" size={15} color={c.green}/><Text style={s.statusText}>{dateLabel(item.scheduledAt)}</Text><Text style={s.note}>{item.status==='active'?'Actif':'En pause'}</Text></View>
    </Card>)}
    <View style={s.notice}><IconCircle name="notifications-outline" bg={c.yellow} color={c.primary}/><View style={{flex:1}}><Text style={s.noticeTitle}>Rappel avant l’envoi</Text><Text style={s.noticeText}>Vous pourrez suspendre l’ordre avant son exécution. Le transfert final sera sécurisé par DOGAA.</Text></View></View>

    <Modal transparent visible={formOpen} animationType="slide" onRequestClose={closeForm}><KeyboardAvoidingView style={s.overlay} behavior={Platform.OS==='ios'?'padding':undefined}><View style={s.sheet}><View style={s.sheetHead}><View><Text style={s.sheetTitle}>Programmer un envoi</Text><Text style={s.sheetText}>Choisissez le destinataire et le moment.</Text></View><TouchableOpacity onPress={closeForm}><Ionicons name="close" size={24}/></TouchableOpacity></View><ScrollView keyboardShouldPersistTaps="handled" showsVerticalScrollIndicator={false}>
      <Text style={s.label}>DESTINATION</Text><View style={s.segment}><Choice active={destination==='person'} icon="person-outline" label="Une personne" onPress={()=>setDestination('person')}/><Choice active={destination==='vault'} icon="lock-closed-outline" label="Mon coffre" onPress={()=>setDestination('vault')}/></View>
      {destination==='person'?<><Text style={s.label}>BÉNÉFICIAIRE</Text><View style={s.inputRow}><TextInput value={recipient} onChangeText={setRecipient} placeholder="Nom de la personne" placeholderTextColor={c.muted} style={[s.input,{flex:1}]}/><TouchableOpacity style={s.contactButton} onPress={openContacts}><Ionicons name="people-outline" size={22} color={c.primary}/></TouchableOpacity></View><TextInput value={phone} onChangeText={setPhone} placeholder="Numéro de téléphone" placeholderTextColor={c.muted} keyboardType="phone-pad" style={s.input}/></>:<><Text style={s.label}>COFFRE À ALIMENTER</Text>{vaults.map(v=><TouchableOpacity key={v.id} onPress={()=>setVaultId(v.id)} style={[s.vaultChoice,vaultId===v.id&&s.vaultActive]}><Ionicons name={vaultId===v.id?'radio-button-on':'radio-button-off'} size={19} color={c.primary}/><Text style={s.vaultText}>{v.name}</Text></TouchableOpacity>)}</>}
      <Text style={s.label}>MONTANT</Text><View style={s.moneyInput}><TextInput value={amount} onChangeText={setAmount} placeholder="0" placeholderTextColor={c.muted} keyboardType="number-pad" style={s.moneyField}/><Text style={s.currency}>FCFA</Text></View>
      <Text style={s.label}>DATE ET HEURE</Text><View style={s.when}><View style={{flex:1}}><Text style={s.hint}>AAAA-MM-JJ</Text><TextInput value={date} onChangeText={setDate} style={s.input}/></View><View style={{width:105}}><Text style={s.hint}>HH:MM</Text><TextInput value={time} onChangeText={setTime} style={s.input}/></View></View>
      <View style={s.confirm}><Ionicons name="information-circle-outline" size={18} color={c.primary}/><Text style={s.confirmText}>L’ordre pourra être mis en pause avant son exécution.</Text></View><PrimaryButton onPress={submit}>Confirmer la programmation</PrimaryButton>
    </ScrollView></View></KeyboardAvoidingView></Modal>

    <Modal transparent visible={contactsOpen} animationType="slide" onRequestClose={()=>setContactsOpen(false)}><View style={s.overlay}><View style={[s.sheet,{height:'78%'}]}><View style={s.sheetHead}><Text style={s.sheetTitle}>Choisir un contact</Text><TouchableOpacity onPress={()=>setContactsOpen(false)}><Ionicons name="close" size={24}/></TouchableOpacity></View><TextInput value={query} onChangeText={setQuery} placeholder="Rechercher un contact" placeholderTextColor={c.muted} style={s.search}/><ScrollView keyboardShouldPersistTaps="handled">{filteredContacts.map(contact=><TouchableOpacity key={contact.id} style={s.contact} onPress={()=>{setRecipient(contact.name);setPhone(contact.phone);setContactsOpen(false);setQuery('');}}><IconCircle name="person-outline"/><View><Text style={s.contactName}>{contact.name}</Text><Text style={s.contactPhone}>{contact.phone}</Text></View></TouchableOpacity>)}</ScrollView></View></View></Modal>
  </Screen>;
}

function Stat({value,label,color=c.white}:{value:string;label:string;color?:string}){return <View style={s.stat}><Text style={[s.statValue,{color}]}>{value}</Text><Text style={s.statLabel}>{label}</Text></View>}
function Choice({active,icon,label,onPress}:{active:boolean;icon:React.ComponentProps<typeof Ionicons>['name'];label:string;onPress:()=>void}){return <TouchableOpacity onPress={onPress} style={[s.choice,active&&s.choiceActive]}><Ionicons name={icon} size={20} color={active?c.white:c.primary}/><Text style={[s.choiceText,active&&{color:c.white}]}>{label}</Text></TouchableOpacity>}
function Empty({icon,text}:{icon:React.ComponentProps<typeof Ionicons>['name'];text:string}){return <View style={s.empty}><Ionicons name={icon} size={42} color={c.primary}/><Text style={s.emptyTitle}>{text}</Text></View>}

const s=StyleSheet.create({
  titleRow:{flexDirection:'row',justifyContent:'space-between',alignItems:'center'},title:{fontSize:22,fontWeight:'800',color:c.primary},scheduler:{height:28,borderRadius:14,paddingHorizontal:10,backgroundColor:c.pale2,alignSelf:'flex-start',flexDirection:'row',alignItems:'center',gap:6,marginTop:8},schedulerText:{fontSize:9,color:c.ink},summary:{backgroundColor:c.primary,borderRadius:12,padding:16,marginVertical:18,flexDirection:'row'},stat:{width:'33.33%',alignItems:'center'},statValue:{fontSize:24,fontWeight:'900'},statLabel:{fontSize:8,fontWeight:'800',color:c.white,marginTop:2},tabs:{height:39,borderRadius:20,backgroundColor:c.pale2,padding:3,flexDirection:'row',marginVertical:18},tab:{flex:1,borderRadius:18,alignItems:'center',justifyContent:'center'},tabActive:{backgroundColor:c.white},tabText:{fontSize:9,color:c.ink},schedule:{marginBottom:11},itemHead:{flexDirection:'row',alignItems:'flex-start',gap:10},info:{flex:1,alignItems:'flex-start'},itemTitle:{fontSize:15,fontWeight:'700',color:c.primary},sub:{fontSize:10,color:c.muted,marginTop:4},pause:{width:36,height:36,borderRadius:18,backgroundColor:c.pale2,alignItems:'center',justifyContent:'center'},amount:{fontSize:25,fontWeight:'800',color:c.primary,marginTop:14},fcfa:{fontSize:10,color:c.yellowDark},status:{borderRadius:8,backgroundColor:c.pale,padding:10,marginTop:12,flexDirection:'row',alignItems:'center',gap:7},statusText:{fontSize:9,color:c.ink,flex:1},note:{fontSize:9,fontWeight:'700',color:c.greenDark},empty:{alignItems:'center',padding:50},emptyTitle:{fontSize:14,color:c.muted,marginTop:12},notice:{padding:16,borderRadius:13,backgroundColor:c.pale2,flexDirection:'row',gap:11},noticeTitle:{fontSize:15,color:c.primary,fontWeight:'700'},noticeText:{fontSize:10,lineHeight:16,color:c.muted,marginTop:4},overlay:{flex:1,backgroundColor:'#00235366',justifyContent:'flex-end'},sheet:{maxHeight:'92%',backgroundColor:c.white,borderTopLeftRadius:27,borderTopRightRadius:27,padding:22,paddingBottom:35},sheetHead:{flexDirection:'row',justifyContent:'space-between',alignItems:'flex-start'},sheetTitle:{fontSize:20,fontWeight:'800',color:c.primary},sheetText:{fontSize:11,color:c.muted,marginTop:4},label:{fontSize:9,fontWeight:'700',color:c.muted,marginTop:16,marginBottom:6},segment:{height:48,borderRadius:13,padding:3,backgroundColor:c.pale2,flexDirection:'row'},choice:{flex:1,borderRadius:11,flexDirection:'row',alignItems:'center',justifyContent:'center',gap:6},choiceActive:{backgroundColor:c.primary},choiceText:{fontSize:11,fontWeight:'700',color:c.primary},inputRow:{flexDirection:'row',gap:8},input:{height:45,borderRadius:10,borderWidth:1,borderColor:c.border,paddingHorizontal:12,color:c.ink,marginBottom:8},contactButton:{width:45,height:45,borderRadius:12,backgroundColor:c.yellow,alignItems:'center',justifyContent:'center'},vaultChoice:{height:42,borderRadius:10,backgroundColor:c.pale,flexDirection:'row',alignItems:'center',paddingHorizontal:10,gap:7,marginBottom:6},vaultActive:{borderWidth:1,borderColor:c.primary},vaultText:{fontSize:11,color:c.ink},moneyInput:{height:48,borderRadius:10,borderWidth:1,borderColor:c.border,flexDirection:'row',alignItems:'center',paddingHorizontal:12},moneyField:{flex:1,fontSize:21,fontWeight:'800',color:c.primary},currency:{fontSize:11,fontWeight:'800',color:c.yellowDark},when:{flexDirection:'row',gap:8},hint:{fontSize:8,color:c.muted},confirm:{backgroundColor:c.pale2,borderRadius:10,padding:10,flexDirection:'row',gap:7,marginBottom:12},confirmText:{fontSize:10,color:c.ink,flex:1},search:{height:45,borderRadius:10,backgroundColor:c.pale,marginVertical:12,paddingHorizontal:12},contact:{flexDirection:'row',alignItems:'center',gap:10,paddingVertical:9},contactName:{fontSize:13,fontWeight:'700',color:c.ink},contactPhone:{fontSize:10,color:c.muted}
});
