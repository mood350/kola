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
  const position=React.useRef(new Animated.Value(-130)).current;
  useEffect(()=>{Animated.spring(position,{toValue:0,useNativeDriver:true,bounciness:7}).start();const timer=setTimeout(()=>Animated.timing(position,{toValue:-130,duration:250,useNativeDriver:true}).start(onClose),4200);return()=>clearTimeout(timer);},[position]);
  const labels:Record<string,string>={P2P_TRANSFER:'Transfert P2P',CASH_IN:'Recharge',CASH_OUT:'Retrait',MERCHANT_PAYMENT:'Paiement marchand',VAULT_DEPOSIT:'Versement au coffre',VAULT_WITHDRAWAL:'Retrait du coffre',BILL_PAYMENT:'Paiement de facture',LOAN_REPAYMENT:'Remboursement'};
  return <Animated.View style={[styles.toast,{transform:[{translateY:position}]}]}><View style={styles.toastIcon}><Ionicons name="checkmark" size={24} color={c.white}/></View><View style={styles.toastCopy}><Text style={styles.toastTitle}>Transaction réussie</Text><Text style={styles.toastText}>{labels[transaction.type]||'Opération DOGAA'} · {new Intl.NumberFormat('fr-FR').format(transaction.amount)} {transaction.currency}</Text>{transaction.counterparty&&<Text numberOfLines={1} style={styles.toastCounterparty}>{transaction.counterparty}</Text>}</View><TouchableOpacity onPress={onClose} style={styles.toastClose}><Ionicons name="close" size={18} color={c.muted}/></TouchableOpacity></Animated.View>;
}

const styles=StyleSheet.create({root:{flex:1},toast:{position:'absolute',zIndex:1000,top:12,left:15,right:15,minHeight:82,borderRadius:18,borderWidth:1,borderColor:'#BDECD7',backgroundColor:c.white,padding:13,flexDirection:'row',alignItems:'center',gap:11,...shadow,elevation:20},toastIcon:{width:46,height:46,borderRadius:23,backgroundColor:c.green,alignItems:'center',justifyContent:'center'},toastCopy:{flex:1},toastTitle:{fontSize:14,fontWeight:'900',color:c.greenDark},toastText:{fontSize:10,fontWeight:'700',color:c.ink,marginTop:4},toastCounterparty:{fontSize:9,color:c.muted,marginTop:2},toastClose:{width:30,height:30,alignItems:'center',justifyContent:'center'}});
