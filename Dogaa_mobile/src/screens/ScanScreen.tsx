import React from 'react';
import { Ionicons } from '@expo/vector-icons';
import { StyleSheet, Text, TouchableOpacity, View } from 'react-native';
import { PrimaryButton, Screen } from '../components/Layout';
import { c } from '../theme';
import { Route } from '../types';

export default function ScanScreen({navigate}:{navigate:(r:Route)=>void}){return <Screen route="scan" navigate={navigate}><Text style={s.title}>Scanner & Payer</Text><Text style={s.sub}>Scannez un QR DOGAA ou marchand pour payer instantanément.</Text><View style={s.camera}><View style={s.cornerTL}/><View style={s.cornerTR}/><View style={s.cornerBL}/><View style={s.cornerBR}/><Ionicons name="qr-code-outline" size={120} color="#FFFFFF55"/><Text style={s.hint}>Placez le QR code dans le cadre</Text></View><PrimaryButton>Activer la caméra</PrimaryButton><TouchableOpacity style={s.manual}><Ionicons name="keypad-outline" size={20} color={c.primary}/><Text style={s.manualText}>Saisir un numéro marchand</Text></TouchableOpacity></Screen>}
const corner={position:'absolute' as const,width:42,height:42,borderColor:c.yellow};
const s=StyleSheet.create({title:{fontSize:24,fontWeight:'800',color:c.primary},sub:{fontSize:11,lineHeight:17,color:c.muted,marginTop:5,marginBottom:20},camera:{height:410,borderRadius:22,backgroundColor:c.primary,alignItems:'center',justifyContent:'center',overflow:'hidden',marginBottom:20},cornerTL:{...corner,top:65,left:45,borderTopWidth:4,borderLeftWidth:4},cornerTR:{...corner,top:65,right:45,borderTopWidth:4,borderRightWidth:4},cornerBL:{...corner,bottom:85,left:45,borderBottomWidth:4,borderLeftWidth:4},cornerBR:{...corner,bottom:85,right:45,borderBottomWidth:4,borderRightWidth:4},hint:{position:'absolute',bottom:40,color:c.white,fontSize:11},manual:{height:48,alignItems:'center',justifyContent:'center',flexDirection:'row',gap:8},manualText:{fontSize:12,fontWeight:'700',color:c.primary}});
