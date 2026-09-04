import React, { createContext, useCallback, useContext, useEffect, useState } from 'react';
import { creditApi, CreditEligibility, DogaaUser, schedulingApi, ScheduledTask, transactionApi, Transaction, userApi, Vault, vaultApi, Wallet, walletApi } from '../services/api';

type Data={user:DogaaUser|null;wallets:Wallet[];vaults:Vault[];transactions:Transaction[];eligibility:CreditEligibility|null;loans:any[];scheduled:ScheduledTask[];loading:boolean;error:string|null;refresh:()=>Promise<void>};
const Context=createContext<Data|null>(null);

export function DogaaDataProvider({children}:{children:React.ReactNode}){
  const [data,setData]=useState<Omit<Data,'loading'|'error'|'refresh'>>({user:null,wallets:[],vaults:[],transactions:[],eligibility:null,loans:[],scheduled:[]});
  const [loading,setLoading]=useState(true),[error,setError]=useState<string|null>(null);
  const refresh=useCallback(async()=>{
    setLoading(true);setError(null);
    try{
      const user=await userApi.me();
      const results=await Promise.allSettled([
        walletApi.list(),vaultApi.list(),transactionApi.history(),creditApi.eligibility(),creditApi.loans(),schedulingApi.list(user.id),
      ]);
      const value=<T,>(index:number,fallback:T)=>results[index].status==='fulfilled'?(results[index] as PromiseFulfilledResult<T>).value:fallback;
      const failed=['portefeuilles','coffres','transactions','éligibilité crédit','prêts','planifications'].filter((_,index)=>results[index].status==='rejected');
      setData({user,wallets:value(0,[]),vaults:value(1,[]),transactions:value<{content:Transaction[]}>(2,{content:[]}).content||[],eligibility:value<CreditEligibility|null>(3,null),loans:value(4,[]),scheduled:value(5,[])});
      if(failed.length)setError(`Modules temporairement indisponibles : ${failed.join(', ')}.`);
    }catch(value){setError(value instanceof Error?value.message:'Impossible de charger les données DOGAA.');}
    finally{setLoading(false);}
  },[]);
  useEffect(()=>{refresh();},[refresh]);
  return <Context.Provider value={{...data,loading,error,refresh}}>{children}</Context.Provider>;
}

export function useDogaaData(){const value=useContext(Context);if(!value)throw new Error('useDogaaData must be used inside DogaaDataProvider');return value;}
