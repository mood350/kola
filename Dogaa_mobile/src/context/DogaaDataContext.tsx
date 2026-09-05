import AsyncStorage from '@react-native-async-storage/async-storage';
import React, { createContext, useCallback, useContext, useEffect, useState } from 'react';
import { AppState } from 'react-native';
import EventSource from 'react-native-sse';
import { API_URL, AuthSession, creditApi, CreditEligibility, DogaaUser, Loan, schedulingApi, ScheduledTask, transactionApi, Transaction, userApi, Vault, vaultApi, Wallet, walletApi } from '../services/api';

type StoredData={user:DogaaUser|null;wallets:Wallet[];vaults:Vault[];transactions:Transaction[];eligibility:CreditEligibility|null;loans:Loan[];scheduled:ScheduledTask[]};
type Data=StoredData&{loading:boolean;error:string|null;refresh:()=>Promise<void>};
const Context=createContext<Data|null>(null);
const initialData:StoredData={user:null,wallets:[],vaults:[],transactions:[],eligibility:null,loans:[],scheduled:[]};

export function DogaaDataProvider({children}:{children:React.ReactNode}){
  const [data,setData]=useState<StoredData>(initialData);
  const [loading,setLoading]=useState(true),[error,setError]=useState<string|null>(null);
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
      source.addEventListener('transaction',()=>load(false));
    }).catch(()=>{});
    return ()=>{cancelled=true;source?.removeAllEventListeners();source?.close();};
  },[load]);

  return <Context.Provider value={{...data,loading,error,refresh}}>{children}</Context.Provider>;
}

export function useDogaaData(){const value=useContext(Context);if(!value)throw new Error('useDogaaData must be used inside DogaaDataProvider');return value;}
