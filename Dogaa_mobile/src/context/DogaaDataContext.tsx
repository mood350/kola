import AsyncStorage from '@react-native-async-storage/async-storage';
import { Ionicons } from '@expo/vector-icons';
import React, { createContext, useCallback, useContext, useEffect, useState } from 'react';
import { Animated, AppState, StyleSheet, Text, TouchableOpacity, View } from 'react-native';
import EventSource from 'react-native-sse';
import { API_URL, AuthSession, creditApi, CreditEligibility, DogaaUser, Loan, schedulingApi, ScheduledTask, transactionApi, Transaction, userApi, Vault, vaultApi, Wallet, walletApi } from '../services/api';
import { c, shadow } from '../theme';

type StoredData={user:DogaaUser|null;wallets:Wallet[];vaults:Vault[];transactions:Transaction[];eligibility:CreditEligibility|null;loans:Loan[];scheduled:ScheduledTask[]};
type Data=StoredData&{loading:boolean;error:string|null;refresh:()=>Promise<void>};
const Context=createContext<Data|null>(null);
const initialData:StoredData={user:null,wallets:[],vaults:[],transactions:[],eligibility:null,loans:[],scheduled:[]};
type TransactionToast={id:string;type:string;amount:number;currency:string;counterparty?:string};

export function DogaaDataProvider({children}:{children:React.ReactNode}){
  const [data,setData]=useState<StoredData>(initialData);
  const [loading,setLoading]=useState(true),[error,setError]=useState<string|null>(null);
  const [toast,setToast]=useState<TransactionToast|null>(null);
  const load=useCallback(async(showLoader:boolean)=>{
    if(showLoader)setLoading(true);
    try{
      const user=await userApi.me();
      const results=await Promise.allSettled([walletApi.list(),vaultApi.list(),transactionApi.history(),creditApi.eligibility(),creditApi.loans(),schedulingApi.list()]);
      const failed=['portefeuilles','coffres','transactions','éligibilité crédit','prêts','planifications'].filter((_,index)=>results[index].status==='rejected');
      setData(current=>{
        const value=<T,>(index:number,fallback:T)=>results[index].status==='fulfilled'?(results[index] as PromiseFulfilledResult<T>).value:fallback;
        return {user,wallets:value(0,current.wallets),vaults:value(1,current.vaults),transactions:value<{content:Transaction[]}>(2,{content:current.transactions}).content||current.transactions,eligibility:value(3,current.eligibility),loans:value(4,current.loans),scheduled:value(5,current.scheduled)};
      });
      setError(failed.length?`Modules temporairement indisponibles : ${failed.join(', ')}.`:null);
    }catch(value){setError(value instanceof Error?value.message:'Impossible de charger les données DOGAA.');}
    finally{if(showLoader)setLoading(false);}
  },[]);
  const refresh=useCallback(()=>load(true),[load]);

  useEffect(()=>{load(true);},[load]);
  useEffect(()=>{
    const interval=setInterval(()=>load(false),15000);
    const appState=AppState.addEventListener('change',state=>{if(state==='active')load(false);});
    return ()=>{clearInterval(interval);appState.remove();};
  },[load]);
  useEffect(()=>{
    let source:EventSource<'transaction'>|null=null,cancelled=false;
    AsyncStorage.getItem('dogaa.session').then(raw=>{
      if(cancelled||!raw)return;
      const session=JSON.parse(raw) as AuthSession;
      source=new EventSource<'transaction'>(`${API_URL}/api/v1/transactions/stream`,{headers:{Authorization:`Bearer ${session.accessToken}`},pollingInterval:5000});
      source.addEventListener('transaction',event=>{try{const transaction=JSON.parse(event.data||'{}') as Transaction;if(transaction.status==='COMPLETED')setToast({id:transaction.reference,type:transaction.type,amount:Number(transaction.amount),currency:transaction.currency,counterparty:transaction.counterparty});}catch{}load(false);});
    }).catch(()=>{});
    return ()=>{cancelled=true;source?.removeAllEventListeners();source?.close();};
  },[load]);

  return <Context.Provider value={{...data,loading,error,refresh}}><View style={styles.root}>{children}{toast&&<SuccessToast transaction={toast} onClose={()=>setToast(null)}/>}</View></Context.Provider>;
}

export function useDogaaData(){const value=useContext(Context);if(!value)throw new Error('useDogaaData must be used inside DogaaDataProvider');return value;}

function SuccessToast({transaction,onClose}:{transaction:TransactionToast;onClose:()=>void}){
  const animation=React.useRef(new Animated.Value(0)).current;
  const close=()=>Animated.timing(animation,{toValue:0,duration:180,useNativeDriver:true}).start(onClose);
  useEffect(()=>{Animated.spring(animation,{toValue:1,useNativeDriver:true,bounciness:8}).start();const timer=setTimeout(close,5200);return()=>clearTimeout(timer);},[animation]);
  const labels:Record<string,string>={P2P_TRANSFER:'Transfert P2P',CASH_IN:'Recharge',CASH_OUT:'Retrait',MERCHANT_PAYMENT:'Paiement marchand',VAULT_DEPOSIT:'Versement au coffre',VAULT_WITHDRAWAL:'Retrait du coffre',BILL_PAYMENT:'Paiement de facture',LOAN_REPAYMENT:'Remboursement'};
  const confetti=[[24,62,c.yellow,15],[55,32,c.blue,-18],[92,73,c.mint,28],[133,38,'#FF6B6B',12],[178,62,c.yellow,-22],[224,31,c.green,18],[265,72,'#9B72FF',-12],[291,42,c.yellow,25],[39,126,'#9B72FF',-22],[278,130,c.blue,15],[70,161,c.green,30],[246,169,'#FF6B6B',-20]] as const;
  return <View style={styles.toastOverlay}><Animated.View style={[styles.toast,{opacity:animation,transform:[{scale:animation.interpolate({inputRange:[0,1],outputRange:[.82,1]})}]}]}>{confetti.map(([left,top,color,rotate],index)=><View key={index} style={[styles.confetti,{left,top,backgroundColor:color,transform:[{rotate:`${rotate}deg`}]}]}/>) }<TouchableOpacity onPress={close} style={styles.toastClose}><Ionicons name="close" size={20} color={c.muted}/></TouchableOpacity><Text style={styles.toastEyebrow}>DOGAA</Text><View style={styles.toastIcon}><Ionicons name="checkmark" size={43} color={c.white}/></View><Text style={styles.toastTitle}>Transaction réussie !</Text><Text style={styles.toastText}>{labels[transaction.type]||'Votre opération'} a été effectuée avec succès.</Text><Text style={styles.toastAmount}>{new Intl.NumberFormat('fr-FR').format(transaction.amount)} <Text style={styles.toastCurrency}>{transaction.currency}</Text></Text>{transaction.counterparty&&<Text numberOfLines={1} style={styles.toastCounterparty}>Avec {transaction.counterparty}</Text>}<View style={styles.toastReceipt}><View style={styles.toastPdf}><Ionicons name="receipt-outline" size={20} color={c.primary}/></View><View style={styles.toastReceiptCopy}><Text style={styles.toastReceiptTitle}>Reçu DOGAA</Text><Text style={styles.toastReceiptRef}>Réf. {transaction.id}</Text></View><Ionicons name="checkmark-circle" size={21} color={c.green}/></View><TouchableOpacity onPress={close} style={styles.toastButton}><Text style={styles.toastButtonText}>Voir le reçu</Text></TouchableOpacity></Animated.View></View>;
}

const styles=StyleSheet.create({root:{flex:1},toastOverlay:{position:'absolute',top:0,right:0,bottom:0,left:0,zIndex:1000,elevation:20,backgroundColor:'#131B2E55',alignItems:'center',justifyContent:'center',padding:22},toast:{width:'100%',maxWidth:360,minHeight:425,borderRadius:22,borderWidth:1,borderColor:'#E8EBF2',backgroundColor:c.white,padding:22,alignItems:'center',overflow:'hidden',...shadow,elevation:20},confetti:{position:'absolute',width:8,height:15,borderRadius:2},toastClose:{position:'absolute',right:13,top:12,width:34,height:34,borderRadius:17,alignItems:'center',justifyContent:'center',backgroundColor:c.pale},toastEyebrow:{fontSize:10,fontWeight:'900',letterSpacing:1.5,color:c.primary},toastIcon:{width:82,height:82,borderRadius:41,backgroundColor:c.green,alignItems:'center',justifyContent:'center',marginTop:35,borderWidth:7,borderColor:'#DDFBED'},toastTitle:{fontSize:21,fontWeight:'900',color:c.ink,textAlign:'center',marginTop:15},toastText:{fontSize:10,lineHeight:15,color:c.muted,textAlign:'center',marginTop:5},toastAmount:{fontSize:27,fontWeight:'900',color:c.primary,marginTop:13},toastCurrency:{fontSize:12,color:c.yellowDark},toastCounterparty:{fontSize:9,color:c.muted,marginTop:3},toastReceipt:{width:'100%',minHeight:61,borderRadius:12,borderWidth:1,borderColor:c.border,backgroundColor:c.pale,flexDirection:'row',alignItems:'center',padding:10,gap:9,marginTop:18},toastPdf:{width:38,height:38,borderRadius:10,backgroundColor:c.white,alignItems:'center',justifyContent:'center'},toastReceiptCopy:{flex:1},toastReceiptTitle:{fontSize:11,fontWeight:'800',color:c.ink},toastReceiptRef:{fontSize:8,color:c.muted,marginTop:3},toastButton:{width:'100%',height:45,borderRadius:23,backgroundColor:c.yellow,alignItems:'center',justifyContent:'center',marginTop:13},toastButtonText:{fontSize:12,fontWeight:'900',color:c.primary}});
