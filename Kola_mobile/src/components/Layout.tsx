import React, { useState } from 'react';
import { Ionicons } from '@expo/vector-icons';
import { Platform, RefreshControl, ScrollView, StyleSheet, Text, TouchableOpacity, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { BlurView } from 'expo-blur';
import { GlassView, isGlassEffectAPIAvailable, isLiquidGlassAvailable } from 'expo-glass-effect';
import { c, shadow } from '../theme';
import { Route } from '../types';
import Logo from './Logo';
import { useKolaData } from '../context/KolaDataContext';

type IconName = React.ComponentProps<typeof Ionicons>['name'];

export function AppHeader({navigate}:{navigate:(r:Route)=>void}) {
  return <View style={s.header}>
    <Logo width={48} style={{height:48}}/>
    <Text style={[s.logo,{marginLeft:2}]}>KOLA</Text>
    <View style={s.spacer}/>
    <View style={s.tier}><Ionicons name="shield-checkmark-outline" size={13} color={c.green}/><Text style={s.tierText}>TIER 2</Text></View>
    <TouchableOpacity style={s.iconButton}><Ionicons name="notifications-outline" size={22} color={c.ink}/><View style={s.alert}/></TouchableOpacity>
    <TouchableOpacity onPress={()=>navigate('profile')} style={s.profile}><Ionicons name="person-outline" size={19} color={c.white}/></TouchableOpacity>
  </View>;
}

const tabs: {route:Route; label:string; icon:IconName}[] = [
  {route:'home',label:'Accueil',icon:'wallet-outline'}, {route:'savings',label:'Bankivi',icon:'business-outline'},
  {route:'scan',label:'',icon:'qr-code-outline'}, {route:'credit',label:'Prêts',icon:'cash-outline'},
  {route:'scheduled',label:'Planifié',icon:'calendar-outline'},
];

export function BottomNav({ route, navigate }: {route:Route; navigate:(r:Route)=>void}) {
  const insets=useSafeAreaInsets();
  const nativeGlass=Platform.OS==='ios'&&isLiquidGlassAvailable()&&isGlassEffectAPIAvailable();
  const items=tabs.map((item,index)=>{
    const active=route===item.route||((route==='loan'||route==='loanDetail')&&item.route==='credit')||(route==='vaultDetail'&&item.route==='vaults');
    return <TouchableOpacity key={item.route} onPress={()=>navigate(item.route)} activeOpacity={.72} style={[s.navItem,index===2&&s.scan,active&&index!==2&&s.navItemActive]}>
      <Ionicons name={item.icon} size={index===2?25:22} color={index===2?c.primary:active?c.primary:c.ink}/>
      {!!item.label&&<Text style={[s.navLabel,active&&s.navLabelActive]}>{item.label}</Text>}
    </TouchableOpacity>;
  });
  return <View pointerEvents="box-none" style={[s.navPosition,{bottom:Math.max(insets.bottom,10)}]}><View style={s.navShadow}>{nativeGlass?<GlassView glassEffectStyle="regular" isInteractive tintColor="rgba(255,255,255,0.18)" style={s.nav}>{items}</GlassView>:<BlurView intensity={Platform.OS==='ios'?72:45} tint="systemUltraThinMaterialLight" blurMethod="dimezisBlurViewSdk31Plus" style={s.nav}>{items}</BlurView>}</View></View>;
}

export function Screen({ children, route, navigate }: {children:React.ReactNode;route:Route;navigate:(r:Route)=>void}) {
  const kola=useKolaData();
  const [refreshing,setRefreshing]=useState(false);
  const refresh=async()=>{setRefreshing(true);try{await kola.refresh();}finally{setRefreshing(false);}};
  return <View style={s.safe}><ScrollView contentContainerStyle={s.content} showsVerticalScrollIndicator={false} alwaysBounceVertical refreshControl={<RefreshControl refreshing={refreshing} onRefresh={refresh} tintColor={c.primary} colors={[c.primary,c.green,c.yellow]} progressBackgroundColor={c.white}/>}>{kola.error&&<TouchableOpacity onPress={kola.refresh} style={s.apiError}><Ionicons name="cloud-offline-outline" size={16} color="#93000A"/><Text style={s.apiErrorText}>{kola.error} Touchez pour réessayer.</Text></TouchableOpacity>}{children}<View style={{height:112}}/></ScrollView><BottomNav route={route} navigate={navigate}/></View>;
}

export function SectionTitle({title,action,onPress}:{title:string;action?:string;onPress?:()=>void}) {
  return <View style={s.sectionRow}><Text style={s.sectionTitle}>{title}</Text>{action&&<TouchableOpacity onPress={onPress}><Text style={s.action}>{action}</Text></TouchableOpacity>}</View>;
}

export function Card({children,style}:{children:React.ReactNode;style?:object}){return <View style={[s.card,style]}>{children}</View>}
export function IconCircle({name,color=c.primary,bg=c.pale2,size=22}:{name:IconName;color?:string;bg?:string;size?:number}){return <View style={[s.iconCircle,{backgroundColor:bg}]}><Ionicons name={name} size={size} color={color}/></View>}
export function Pill({children,green=false}:{children:React.ReactNode;green?:boolean}){return <View style={[s.pill,green&&s.pillGreen]}><Text style={[s.pillText,green&&s.pillGreenText]}>{children}</Text></View>}
export function Progress({value,color=c.green}:{value:number;color?:string}){return <View style={s.progress}><View style={[s.progressFill,{width:`${Math.min(value,100)}%`,backgroundColor:color}]}/></View>}
export function PrimaryButton({children,onPress,blue=false}:{children:React.ReactNode;onPress?:()=>void;blue?:boolean}){return <TouchableOpacity onPress={onPress} style={[s.primaryButton,blue&&s.blueButton]}><Text style={[s.primaryButtonText,blue&&s.blueButtonText]}>{children}</Text></TouchableOpacity>}

const s=StyleSheet.create({
  apiError:{padding:10,borderRadius:10,backgroundColor:'#FFDAD6',flexDirection:'row',alignItems:'center',gap:7,marginBottom:12},apiErrorText:{fontSize:9,lineHeight:13,color:'#93000A',flex:1},
  safe:{flex:1,backgroundColor:c.bg},header:{height:64,paddingHorizontal:20,flexDirection:'row',alignItems:'center',backgroundColor:'rgba(250,248,255,0.98)'},logoMark:{width:10,height:10,borderRadius:3,backgroundColor:c.primary,alignItems:'center',justifyContent:'center',marginRight:9},logoLetter:{fontSize:5,color:c.yellow,fontWeight:'900'},logo:{fontSize:17,fontWeight:'800',color:c.primary,letterSpacing:.4},spacer:{flex:1},tier:{height:27,paddingHorizontal:10,borderRadius:14,backgroundColor:c.pale2,flexDirection:'row',alignItems:'center',gap:4},tierText:{fontSize:9,fontWeight:'800',color:c.ink},iconButton:{width:37,height:40,alignItems:'center',justifyContent:'center',marginLeft:4},alert:{position:'absolute',top:6,right:7,width:7,height:7,borderRadius:4,backgroundColor:c.yellow},profile:{width:38,height:38,borderRadius:20,backgroundColor:c.primary,alignItems:'center',justifyContent:'center'},content:{paddingHorizontal:20,paddingTop:15},navPosition:{position:'absolute',left:14,right:14},navShadow:{height:68,borderRadius:34,shadowColor:'#071B52',shadowOpacity:.18,shadowRadius:18,shadowOffset:{width:0,height:8},elevation:14},nav:{height:68,borderRadius:34,overflow:'hidden',borderWidth:1,borderColor:'rgba(255,255,255,0.72)',backgroundColor:Platform.OS==='ios'?'rgba(255,255,255,0.12)':'rgba(255,255,255,0.88)',flexDirection:'row',alignItems:'center',paddingHorizontal:5},navItem:{flex:1,alignItems:'center',justifyContent:'center',height:54,borderRadius:27},navItemActive:{backgroundColor:'rgba(14,51,117,0.10)'},navLabel:{fontSize:9,color:c.ink,marginTop:3},navLabelActive:{fontWeight:'800',color:c.primary},scan:{flex:0,width:56,height:56,borderRadius:28,backgroundColor:c.yellow,marginHorizontal:5,shadowColor:c.yellow,shadowOpacity:.38,shadowRadius:10,shadowOffset:{width:0,height:4},elevation:8},sectionRow:{flexDirection:'row',alignItems:'center',justifyContent:'space-between',marginTop:24,marginBottom:13},sectionTitle:{fontSize:20,fontWeight:'800',color:c.ink,letterSpacing:-.3},action:{fontSize:10,fontWeight:'700',color:c.primary},card:{backgroundColor:c.white,borderRadius:16,padding:16,borderWidth:1,borderColor:'#EDF0F5',...shadow},iconCircle:{width:43,height:43,borderRadius:22,alignItems:'center',justifyContent:'center'},pill:{paddingHorizontal:8,paddingVertical:4,borderRadius:12,backgroundColor:c.pale2},pillText:{fontSize:8,fontWeight:'700',color:c.primary},pillGreen:{backgroundColor:'#DDFBED'},pillGreenText:{color:c.greenDark},progress:{height:7,borderRadius:4,backgroundColor:'#E0E5FA',overflow:'hidden'},progressFill:{height:'100%',borderRadius:4},primaryButton:{height:48,borderRadius:25,backgroundColor:c.yellow,alignItems:'center',justifyContent:'center',shadowColor:c.yellow,shadowOpacity:.25,shadowRadius:9,elevation:3},primaryButtonText:{fontSize:14,fontWeight:'800',color:c.primary},blueButton:{backgroundColor:c.primary},blueButtonText:{color:c.white}
});
