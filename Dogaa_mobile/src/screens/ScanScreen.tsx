import { Ionicons } from '@expo/vector-icons';
import { CameraView, useCameraPermissions } from 'expo-camera';
import React, { useEffect, useState } from 'react';
import { Alert, Linking, StyleSheet, Text, TouchableOpacity, View } from 'react-native';
import { Screen } from '../components/Layout';
import { c } from '../theme';
import { Route } from '../types';

export default function ScanScreen({navigate}:{navigate:(route:Route)=>void}){
  const [permission,requestPermission]=useCameraPermissions();
  const [scanned,setScanned]=useState(false);

  useEffect(()=>{if(permission&&!permission.granted&&permission.canAskAgain)requestPermission();},[permission,requestPermission]);
  const askForCamera=async()=>{const result=await requestPermission();if(!result.granted&&!result.canAskAgain)Alert.alert('Caméra désactivée','Autorisez DOGAA à utiliser la caméra dans les réglages du téléphone.',[{text:'Annuler',style:'cancel'},{text:'Ouvrir les réglages',onPress:()=>Linking.openSettings()}]);};
  const onScanned=({data}:{data:string})=>{if(scanned)return;setScanned(true);Alert.alert('QR code détecté',data,[{text:'Scanner à nouveau',onPress:()=>setScanned(false)},{text:'Continuer'}]);};

  return <Screen route="scan" navigate={navigate}>
    <Text style={s.title}>Scanner & Payer</Text><Text style={s.sub}>Scannez un QR DOGAA ou marchand pour payer instantanément.</Text>
    <View style={s.camera}>
      {permission?.granted?<CameraView style={StyleSheet.absoluteFill} facing="back" barcodeScannerSettings={{barcodeTypes:['qr']}} onBarcodeScanned={scanned?undefined:onScanned}/>:<View style={s.permission}><Ionicons name="camera-outline" size={58} color={c.white}/><Text style={s.permissionTitle}>{permission?'Accès à la caméra requis':'Ouverture de la caméra…'}</Text><Text style={s.permissionText}>DOGAA a besoin de la caméra pour lire le QR code du bénéficiaire.</Text></View>}
      <View pointerEvents="none" style={s.scanFrame}><View style={s.cornerTL}/><View style={s.cornerTR}/><View style={s.cornerBL}/><View style={s.cornerBR}/></View>
      {permission?.granted&&<Text style={s.hint}>{scanned?'QR code détecté':'Placez le QR code dans le cadre'}</Text>}
    </View>
    {!permission?.granted&&<TouchableOpacity onPress={permission&&!permission.canAskAgain?()=>Linking.openSettings():askForCamera} style={s.cameraButton}><Ionicons name="camera-outline" size={20} color={c.primary}/><Text style={s.cameraButtonText}>{permission&&!permission.canAskAgain?'Ouvrir les réglages':'Autoriser la caméra'}</Text></TouchableOpacity>}
    {permission?.granted&&scanned&&<TouchableOpacity onPress={()=>setScanned(false)} style={s.cameraButton}><Ionicons name="scan-outline" size={20} color={c.primary}/><Text style={s.cameraButtonText}>Scanner à nouveau</Text></TouchableOpacity>}
    <TouchableOpacity style={s.manual}><Ionicons name="keypad-outline" size={20} color={c.primary}/><Text style={s.manualText}>Saisir un numéro marchand</Text></TouchableOpacity>
  </Screen>;
}

const corner={position:'absolute' as const,width:42,height:42,borderColor:c.yellow};
const s=StyleSheet.create({title:{fontSize:24,fontWeight:'800',color:c.primary},sub:{fontSize:11,lineHeight:17,color:c.muted,marginTop:5,marginBottom:20},camera:{height:410,borderRadius:22,backgroundColor:c.primary,alignItems:'center',justifyContent:'center',overflow:'hidden',marginBottom:20},permission:{alignItems:'center',paddingHorizontal:35},permissionTitle:{fontSize:16,fontWeight:'800',color:c.white,marginTop:13},permissionText:{fontSize:11,lineHeight:17,color:'#DCE4FF',textAlign:'center',marginTop:7},scanFrame:{position:'absolute',width:230,height:230},cornerTL:{...corner,top:0,left:0,borderTopWidth:4,borderLeftWidth:4,borderTopLeftRadius:10},cornerTR:{...corner,top:0,right:0,borderTopWidth:4,borderRightWidth:4,borderTopRightRadius:10},cornerBL:{...corner,bottom:0,left:0,borderBottomWidth:4,borderLeftWidth:4,borderBottomLeftRadius:10},cornerBR:{...corner,bottom:0,right:0,borderBottomWidth:4,borderRightWidth:4,borderBottomRightRadius:10},hint:{position:'absolute',bottom:24,backgroundColor:'#002353CC',borderRadius:15,paddingHorizontal:13,paddingVertical:7,fontSize:10,color:c.white},cameraButton:{height:48,borderRadius:24,backgroundColor:c.yellow,flexDirection:'row',alignItems:'center',justifyContent:'center',gap:8},cameraButtonText:{fontSize:13,fontWeight:'800',color:c.primary},manual:{height:50,marginTop:10,borderRadius:25,borderWidth:1,borderColor:c.border,flexDirection:'row',alignItems:'center',justifyContent:'center',gap:8},manualText:{fontSize:12,fontWeight:'700',color:c.primary}});
