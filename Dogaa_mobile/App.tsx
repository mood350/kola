import AsyncStorage from '@react-native-async-storage/async-storage';
import React, { useEffect, useState } from 'react';
import { StatusBar } from 'expo-status-bar';
import { SafeAreaProvider } from 'react-native-safe-area-context';
import { ActivityIndicator, StyleSheet, View } from 'react-native';
import HomeScreen from './src/screens/HomeScreen';
import VaultsScreen from './src/screens/VaultsScreen';
import CreditScreen from './src/screens/CreditScreen';
import LoanScreen from './src/screens/LoanScreen';
import LoanDetailScreen from './src/screens/LoanDetailScreen';
import ProfileScreen from './src/screens/ProfileScreen';
import KycScreen from './src/screens/KycScreen';
import TransactionHistoryScreen from './src/screens/TransactionHistoryScreen';
import OnboardingScreen from './src/screens/OnboardingScreen';
import AuthScreen from './src/screens/AuthScreen';
import ScheduledScreen from './src/screens/ScheduledScreen';
import ScanScreen from './src/screens/ScanScreen';
import { Route } from './src/types';

export default function App(){
  const [route,setRoute]=useState<Route>('home');
  const [entry,setEntry]=useState<'loading'|'onboarding'|'auth'|'app'>('loading');
  useEffect(()=>{Promise.all([AsyncStorage.getItem('dogaa.onboarding.seen'),AsyncStorage.getItem('dogaa.session')]).then(([seen,session])=>setEntry(session?'app':seen?'auth':'onboarding')).catch(()=>setEntry('onboarding'));},[]);
  const finishOnboarding=async()=>{await AsyncStorage.setItem('dogaa.onboarding.seen','true');setEntry('auth');};
  const authenticate=async()=>{await AsyncStorage.setItem('dogaa.session','demo-session');setEntry('app');};
  const screens:Record<Route,React.ReactNode>={home:<HomeScreen navigate={setRoute}/>,vaults:<VaultsScreen navigate={setRoute}/>,scan:<ScanScreen navigate={setRoute}/>,credit:<CreditScreen navigate={setRoute}/>,loan:<LoanScreen navigate={setRoute}/>,loanDetail:<LoanDetailScreen navigate={setRoute}/>,profile:<ProfileScreen navigate={setRoute}/>,kyc:<KycScreen navigate={setRoute}/>,transactionHistory:<TransactionHistoryScreen navigate={setRoute}/>,scheduled:<ScheduledScreen navigate={setRoute}/>};
  return <SafeAreaProvider><StatusBar style="dark"/>{entry==='loading'?<View style={s.loading}><ActivityIndicator color="#002353"/></View>:entry==='onboarding'?<OnboardingScreen onFinish={finishOnboarding}/>:entry==='auth'?<AuthScreen onAuthenticated={authenticate}/>:screens[route]}</SafeAreaProvider>;
}
const s=StyleSheet.create({loading:{flex:1,alignItems:'center',justifyContent:'center',backgroundColor:'#FAF8FF'}});
