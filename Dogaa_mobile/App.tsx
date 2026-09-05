import AsyncStorage from '@react-native-async-storage/async-storage';
import React, { useEffect, useState } from 'react';
import { StatusBar } from 'expo-status-bar';
import { SafeAreaProvider } from 'react-native-safe-area-context';
import HomeScreen from './src/screens/HomeScreen';
import VaultsScreen from './src/screens/VaultsScreen';
import VaultDetailScreen from './src/screens/VaultDetailScreen';
import CreditScreen from './src/screens/CreditScreen';
import LoanScreen from './src/screens/LoanScreen';
import LoanDetailScreen from './src/screens/LoanDetailScreen';
import ProfileScreen from './src/screens/ProfileScreen';
import FaqScreen from './src/screens/FaqScreen';
import KycScreen from './src/screens/KycScreen';
import TransactionHistoryScreen from './src/screens/TransactionHistoryScreen';
import OnboardingScreen from './src/screens/OnboardingScreen';
import AuthScreen from './src/screens/AuthScreen';
import ScheduledScreen from './src/screens/ScheduledScreen';
import ScanScreen from './src/screens/ScanScreen';
import { Route } from './src/types';
import SplashScreen from './src/screens/SplashScreen';
import { authApi, AuthSession } from './src/services/api';
import { DogaaDataProvider } from './src/context/DogaaDataContext';

export default function App(){
  const [route,setRoute]=useState<Route>('home');
  const [selectedVaultId,setSelectedVaultId]=useState<string|null>(null);
  const [entry,setEntry]=useState<'loading'|'onboarding'|'auth'|'app'>('loading');
  useEffect(()=>{Promise.all([AsyncStorage.getItem('dogaa.onboarding.seen'),AsyncStorage.getItem('dogaa.session'),new Promise(resolve=>setTimeout(resolve,2600))]).then(([seen,session])=>setEntry(session?'app':seen?'auth':'onboarding')).catch(()=>setEntry('onboarding'));},[]);
  const finishOnboarding=async()=>{await AsyncStorage.setItem('dogaa.onboarding.seen','true');setEntry('auth');};
  const authenticate=async(session:AuthSession)=>{await AsyncStorage.setItem('dogaa.session',JSON.stringify(session));setEntry('app');};
  const logout=async()=>{const saved=await AsyncStorage.getItem('dogaa.session');try{if(saved){const session=JSON.parse(saved) as AuthSession;await authApi.logout(session.refreshToken);}}catch{}finally{await AsyncStorage.removeItem('dogaa.session');setRoute('home');setEntry('auth');}};
  const openVault=(id:string)=>{setSelectedVaultId(id);setRoute('vaultDetail');};
  const screens:Record<Route,React.ReactNode>={home:<HomeScreen navigate={setRoute}/>,vaults:<VaultsScreen navigate={setRoute} onOpenVault={openVault}/>,vaultDetail:<VaultDetailScreen navigate={setRoute} vaultId={selectedVaultId}/>,scan:<ScanScreen navigate={setRoute}/>,credit:<CreditScreen navigate={setRoute}/>,loan:<LoanScreen navigate={setRoute}/>,loanDetail:<LoanDetailScreen navigate={setRoute}/>,profile:<ProfileScreen navigate={setRoute} onLogout={logout}/>,faq:<FaqScreen navigate={setRoute}/>,kyc:<KycScreen navigate={setRoute}/>,transactionHistory:<TransactionHistoryScreen navigate={setRoute}/>,scheduled:<ScheduledScreen navigate={setRoute}/>};
  return <SafeAreaProvider><StatusBar style={entry==='loading'?'light':'dark'}/>{entry==='loading'?<SplashScreen/>:entry==='onboarding'?<OnboardingScreen onFinish={finishOnboarding}/>:entry==='auth'?<AuthScreen onAuthenticated={authenticate}/>:<DogaaDataProvider>{screens[route]}</DogaaDataProvider>}</SafeAreaProvider>;
}
