import { Ionicons } from '@expo/vector-icons';
import { CameraView, useCameraPermissions } from 'expo-camera';
import React, { useEffect, useState } from 'react';
import { Alert, Linking, StyleSheet, Text, TextInput, TouchableOpacity, View } from 'react-native';
import { Card, Screen } from '../components/Layout';
import { useDogaaData } from '../context/DogaaDataContext';
import { FeeQuote, transactionApi } from '../services/api';
import { c } from '../theme';
import { Route } from '../types';

const money=(value:number)=>new Intl.NumberFormat('fr-FR').format(value);
const togolesePhone=(payload:string)=>{
  let value=payload.trim();
  try{const parsed=JSON.parse(value) as Record<string,unknown>;value=String(parsed.recipientPhone||parsed.phone||parsed.account||'');}catch{}
  const query=value.match(/[?&](?:recipientPhone|phone)=([^&]+)/i)?.[1];
  if(query)value=decodeURIComponent(query);
  const digits=value.replace(/\D/g,'');
  if(digits.length===8)return `+228${digits}`;
  if(digits.length===11&&digits.startsWith('228'))return `+${digits}`;
  return null;
};

export default function ScanScreen({navigate}:{navigate:(route:Route)=>void}){
  const dogaa=useDogaaData();
  const [permission,requestPermission]=useCameraPermissions();
  const [scanned,setScanned]=useState(false),[phone,setPhone]=useState(''),[amount,setAmount]=useState(''),[description,setDescription]=useState(''),[quote,setQuote]=useState<FeeQuote|null>(null),[sending,setSending]=useState(false);
  useEffect(()=>{if(permission&&!permission.granted&&permission.canAskAgain)requestPermission();},[permission,requestPermission]);
  const askForCamera=async()=>{const result=await requestPermission();if(!result.granted&&!result.canAskAgain)Alert.alert('Caméra désactivée','Autorisez DOGAA à utiliser la caméra dans les réglages du téléphone.',[{text:'Annuler',style:'cancel'},{text:'Ouvrir les réglages',onPress:()=>Linking.openSettings()}]);};
  const reset=()=>{setScanned(false);setPhone('');setAmount('');setDescription('');setQuote(null);};
  const onScanned=({data}:{data:string})=>{if(scanned)return;setScanned(true);const recipient=togolesePhone(data);if(!recipient){Alert.alert('QR code non reconnu','Ce QR ne contient pas un numéro DOGAA togolais valide.',[{text:'Scanner à nouveau',onPress:reset}]);return;}setPhone(recipient);};
  const prepare=async()=>{const value=Number(amount.replace(/\D/g,''));if(!phone){Alert.alert('Bénéficiaire requis','Scannez le QR DOGAA du bénéficiaire.');return;}if(value<=0){Alert.alert('Montant invalide','Saisissez un montant supérieur à zéro.');return;}try{setQuote(await transactionApi.quote(value));}catch(error){Alert.alert('Calcul impossible',error instanceof Error?error.message:'Erreur serveur.');}};
  const send=async()=>{if(!quote)return;setSending(true);try{await transactionApi.transfer({amount:Number(quote.amount),recipientPhone:phone,description:description.trim()||undefined});await dogaa.refresh();reset();navigate('home');}catch(error){Alert.alert('Transfert impossible',error instanceof Error?error.message:'Erreur serveur.');}finally{setSending(false);}};

  return <Screen route="scan" navigate={navigate}>
    <Text style={s.title}>Envoyer par QR</Text><Text style={s.sub}>Scannez le QR DOGAA du bénéficiaire, puis indiquez le montant.</Text>
    {!phone?<View style={s.camera}>
      {permission?.granted?<CameraView style={StyleSheet.absoluteFill} facing="back" barcodeScannerSettings={{barcodeTypes:['qr']}} onBarcodeScanned={scanned?undefined:onScanned}/>:<View style={s.permission}><Ionicons name="camera-outline" size={58} color={c.white}/><Text style={s.permissionTitle}>{permission?'Accès à la caméra requis':'Ouverture de la caméra…'}</Text><Text style={s.permissionText}>DOGAA a besoin de la caméra pour lire le QR code du bénéficiaire.</Text></View>}
      <View pointerEvents="none" style={s.scanFrame}><View style={s.cornerTL}/><View style={s.cornerTR}/><View style={s.cornerBL}/><View style={s.cornerBR}/></View>
      {permission?.granted&&<Text style={s.hint}>Placez le QR DOGAA dans le cadre</Text>}
    </View>:<Card style={s.recipient}><View style={s.recipientIcon}><Ionicons name="person-outline" size={25} color={c.primary}/></View><View style={s.recipientCopy}><Text style={s.recipientLabel}>BÉNÉFICIAIRE DÉTECTÉ</Text><Text style={s.recipientPhone}>{phone}</Text></View><TouchableOpacity onPress={reset}><Ionicons name="scan-outline" size={22} color={c.primary}/></TouchableOpacity></Card>}
    {!permission?.granted&&!phone&&<TouchableOpacity onPress={permission&&!permission.canAskAgain?()=>Linking.openSettings():askForCamera} style={s.primary}><Ionicons name="camera-outline" size={20} color={c.primary}/><Text style={s.primaryText}>{permission&&!permission.canAskAgain?'Ouvrir les réglages':'Autoriser la caméra'}</Text></TouchableOpacity>}
    {phone&&<View style={s.form}><Text style={s.label}>MONTANT À ENVOYER</Text><View style={s.amountBox}><TextInput autoFocus value={amount} onChangeText={value=>{setAmount(value.replace(/\D/g,''));setQuote(null);}} keyboardType="number-pad" placeholder="0" placeholderTextColor={c.muted} style={s.amountInput}/><Text style={s.currency}>FCFA</Text></View><Text style={s.label}>MOTIF (FACULTATIF)</Text><TextInput value={description} onChangeText={setDescription} onFocus={()=>setQuote(null)} maxLength={140} placeholder="Ex. Remboursement, cadeau…" placeholderTextColor={c.muted} style={s.description}/>
      {quote&&<Card style={s.summary}><Row label="Montant" value={`${money(Number(quote.amount))} FCFA`}/><Row label="Frais DOGAA" value={`${money(Number(quote.fee))} FCFA`}/><View style={s.total}><Text style={s.totalLabel}>TOTAL À DÉBITER</Text><Text style={s.totalValue}>{money(Number(quote.total))} FCFA</Text></View></Card>}
      <TouchableOpacity disabled={sending} onPress={quote?send:prepare} style={[s.primary,sending&&{opacity:.6}]}><Ionicons name={sending?'hourglass-outline':quote?'send-outline':'calculator-outline'} size={20} color={c.primary}/><Text style={s.primaryText}>{sending?'Envoi en cours…':quote?'Confirmer le transfert':'Voir le récapitulatif'}</Text></TouchableOpacity>
    </View>}
    <View style={s.secure}><Ionicons name="shield-checkmark-outline" size={18} color={c.greenDark}/><Text style={s.secureText}>Vérifiez toujours le numéro et le montant avant de confirmer.</Text></View>
  </Screen>;
}

function Row({label,value}:{label:string;value:string}){return <View style={s.row}><Text style={s.rowLabel}>{label}</Text><Text style={s.rowValue}>{value}</Text></View>}
const corner={position:'absolute' as const,width:42,height:42,borderColor:c.yellow};
const s=StyleSheet.create({title:{fontSize:24,fontWeight:'800',color:c.primary},sub:{fontSize:11,lineHeight:17,color:c.muted,marginTop:5,marginBottom:20},camera:{height:410,borderRadius:22,backgroundColor:c.primary,alignItems:'center',justifyContent:'center',overflow:'hidden',marginBottom:20},permission:{alignItems:'center',paddingHorizontal:35},permissionTitle:{fontSize:16,fontWeight:'800',color:c.white,marginTop:13},permissionText:{fontSize:11,lineHeight:17,color:'#DCE4FF',textAlign:'center',marginTop:7},scanFrame:{position:'absolute',width:230,height:230},cornerTL:{...corner,top:0,left:0,borderTopWidth:4,borderLeftWidth:4,borderTopLeftRadius:10},cornerTR:{...corner,top:0,right:0,borderTopWidth:4,borderRightWidth:4,borderTopRightRadius:10},cornerBL:{...corner,bottom:0,left:0,borderBottomWidth:4,borderLeftWidth:4,borderBottomLeftRadius:10},cornerBR:{...corner,bottom:0,right:0,borderBottomWidth:4,borderRightWidth:4,borderBottomRightRadius:10},hint:{position:'absolute',bottom:24,backgroundColor:'#002353CC',borderRadius:15,paddingHorizontal:13,paddingVertical:7,fontSize:10,color:c.white},primary:{height:50,borderRadius:25,backgroundColor:c.yellow,flexDirection:'row',alignItems:'center',justifyContent:'center',gap:8,marginTop:16},primaryText:{fontSize:13,fontWeight:'900',color:c.primary},recipient:{flexDirection:'row',alignItems:'center',gap:11},recipientIcon:{width:48,height:48,borderRadius:24,backgroundColor:c.pale2,alignItems:'center',justifyContent:'center'},recipientCopy:{flex:1},recipientLabel:{fontSize:8,fontWeight:'800',color:c.greenDark},recipientPhone:{fontSize:17,fontWeight:'900',color:c.ink,marginTop:4},form:{marginTop:5},label:{fontSize:9,fontWeight:'800',color:c.muted,marginTop:17,marginBottom:7},amountBox:{height:55,borderWidth:1,borderColor:c.border,borderRadius:13,backgroundColor:c.white,paddingHorizontal:14,flexDirection:'row',alignItems:'center'},amountInput:{flex:1,fontSize:24,fontWeight:'900',color:c.primary},currency:{fontSize:11,fontWeight:'900',color:c.yellowDark},description:{height:49,borderWidth:1,borderColor:c.border,borderRadius:13,backgroundColor:c.white,paddingHorizontal:14,color:c.ink},summary:{marginTop:15,paddingVertical:5},row:{height:40,flexDirection:'row',alignItems:'center',borderBottomWidth:1,borderBottomColor:c.border},rowLabel:{fontSize:10,color:c.muted,flex:1},rowValue:{fontSize:10,fontWeight:'800',color:c.ink},total:{height:48,flexDirection:'row',alignItems:'center'},totalLabel:{fontSize:9,fontWeight:'900',color:c.primary,flex:1},totalValue:{fontSize:15,fontWeight:'900',color:c.primary},secure:{borderRadius:12,backgroundColor:'#DDFBED',padding:13,flexDirection:'row',alignItems:'center',gap:8,marginTop:15},secureText:{fontSize:9,lineHeight:14,color:c.muted,flex:1}});
