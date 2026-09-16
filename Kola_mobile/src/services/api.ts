const API_URL=(process.env.EXPO_PUBLIC_API_URL||'http://10.0.2.2:8080').replace(/\/$/,'');

type ApiEnvelope<T>={success:boolean;message?:string;data:T};
type ApiErrorBody={message?:string;fieldErrors?:Record<string,string>};

export type KolaUser={
  id:string;firstName:string;lastName:string;phone:string;email?:string;
  kycTier:string;phoneVerified:boolean;emailVerified:boolean;
};
export type AuthSession={accessToken:string;refreshToken:string;tokenType:string;expiresIn:number;user:KolaUser};
export type RecipientLookup={id:string;displayName:string;maskedPhone:string};
export type TransferRecipientLookup={phone:string;phoneMasked:string;registered:boolean;name?:string;self:boolean};
export type KycDocument={id:string;type:'NATIONAL_ID'|'PASSPORT'|'DRIVING_LICENCE'|'VOTER_CARD'|'SELFIE'|'PROOF_OF_ADDRESS';status:'PENDING'|'APPROVED'|'REJECTED';originalFilename:string;contentType:string;sizeBytes:number;rejectionReason?:string;reviewedAt?:string;submittedAt:string};
export type KycStatus={tier:string;nextTier?:string;phoneVerified:boolean;profileComplete:boolean;identityDocumentApproved:boolean;requirementsForNextTier:string[];documents:KycDocument[];limits:{perTransaction:number;daily:number;monthly:number}};

export class ApiError extends Error{
  constructor(message:string,public status:number,public fieldErrors?:Record<string,string>){super(message);}
}

async function request<T>(path:string,options:RequestInit={},authenticated=false,retry=true):Promise<T>{
  const saved=authenticated?await AsyncStorage.getItem('kola.session'):null;
  const session=saved?JSON.parse(saved) as AuthSession:null;
  let response:Response;
  try{
    response=await fetch(`${API_URL}${path}`,{...options,headers:{Accept:'application/json',...(options.body instanceof FormData?{}:{'Content-Type':'application/json'}),...(session?{Authorization:`Bearer ${session.accessToken}`}:{ }),...options.headers}});
  }catch{
    throw new ApiError(`Serveur KOLA inaccessible (${API_URL}). Vérifiez l'adresse EXPO_PUBLIC_API_URL.`,0);
  }
  if(response.status===401&&authenticated&&retry&&session?.refreshToken){
    const refreshed=await authApi.refresh(session.refreshToken);
    await AsyncStorage.setItem('kola.session',JSON.stringify(refreshed));
    return request<T>(path,options,true,false);
  }
  const body=await response.json().catch(()=>({})) as ApiEnvelope<T>&ApiErrorBody;
  if(!response.ok)throw new ApiError(body.message||'Une erreur est survenue.',response.status,body.fieldErrors);
  return Object.prototype.hasOwnProperty.call(body,'success')?body.data:body as T;
}

export const authApi={
  requestOtp:(phone:string)=>request<{phone:string;codeExpiresInSeconds:number;resendAvailableAt:string}>('/api/v1/auth/register/request-otp',{method:'POST',body:JSON.stringify({phone})}),
  verifyOtp:(phone:string,code:string)=>request<{verificationToken:string;expiresInSeconds:number}>('/api/v1/auth/register/verify-otp',{method:'POST',body:JSON.stringify({phone,code})}),
  register:(payload:{verificationToken:string;firstName:string;lastName:string;email?:string;dateOfBirth:string;pin:string;confirmPin:string;acceptedPrivacyPolicy:boolean})=>request<AuthSession>('/api/v1/auth/register',{method:'POST',body:JSON.stringify({...payload,country:'TG'})}),
  login:(phone:string,pin:string)=>request<AuthSession>('/api/v1/auth/login',{method:'POST',body:JSON.stringify({phone,pin})}),
  refresh:(refreshToken:string)=>request<AuthSession>('/api/v1/auth/refresh',{method:'POST',body:JSON.stringify({refreshToken})}),
  logout:(refreshToken:string)=>request<void>('/api/v1/auth/logout',{method:'POST',body:JSON.stringify({refreshToken})}),
};

export type Wallet={id:string;currency:string;type?:'CURRENT'|'SAVINGS';availableBalance:number;lockedBalance:number;totalBalance:number;status:string;createdAt:string};
export type Vault={id:string;name:string;currency:string;balance:number;targetAmount:number;targetDate?:string;progressPercent:number;goalReached:boolean;status:string;description?:string;createdAt:string};
export type Transaction={id:string;reference:string;type:string;status:string;currency:string;amount:number;fee:number;totalDebited:number;counterparty?:string;description?:string;failureReason?:string;completedAt?:string;createdAt:string};
export type FeeQuote={currency:string;amount:number;fee:number;total:number};
export type Biller={code:string;displayName:string;identifierLabel:string;identifierKind:'DIGITS'|'ALPHANUMERIC';minLength:number;maxLength:number;fixedAmount:boolean};
export type ScheduledTask={id:string;userId:string;type:string;frequency:string;amount:number;currency:string;beneficiaryReference:string;description?:string;status:string;nextRunAt:string;endDate?:string;maxOccurrences?:number;occurrencesCompleted:number;retryCount:number;lastFailureReason?:string;fundingVaultId?:string;biller?:string;dayOfMonth?:number};
export type CreditEligibility={eligible:boolean;score:number;minimumScore:number;kycTier:string;currency:string;savingsBalance:number;leverageRatio:number;maxLoanAmount:number;monthlyRatePercent:number;totalRepayable:number;termDays:number;loansRepaid:number;blockers:string[]};
export type Loan={id:string;currency:string;principal:number;collateralAmount:number;leverageRatio:number;monthlyRatePercent:number;interestAmount:number;penaltyAmount:number;totalDue:number;amountRepaid:number;outstanding:number;shortfallAmount:number;status:string;scoreAtGrant:number;disbursedAt:string;dueAt:string;settledAt?:string};
export type AssistantMessage={id:string;role:'USER'|'ASSISTANT';content:string;createdAt:string};
export type AssistantConversation={id:string;title:string;lastMessageAt:string};
export type AssistantConversationDetail=AssistantConversation&{messages:AssistantMessage[]};
export type AssistantReply={conversationId:string;title:string;answer:AssistantMessage;remainingToday:number};

export const userApi={me:()=>request<KolaUser>('/api/v1/users/me',{},true),lookupRecipient:(phone:string)=>request<RecipientLookup>(`/api/v1/users/recipients/${encodeURIComponent(phone)}`,{},true)};
export const walletApi={list:()=>request<Wallet[]>('/api/v1/wallets',{},true),deposit:(amount:number)=>request<Wallet>('/api/v1/wallets/XOF/deposit',{method:'POST',body:JSON.stringify({amount})},true),depositToSavings:(amount:number)=>request<Wallet[]>('/api/v1/wallets/savings/deposit',{method:'POST',body:JSON.stringify({currency:'XOF',amount})},true),withdrawFromSavings:(amount:number)=>request<Wallet[]>('/api/v1/wallets/savings/withdraw',{method:'POST',body:JSON.stringify({currency:'XOF',amount})},true)};
export const vaultApi={list:()=>request<Vault[]>('/api/v1/vaults',{},true),create:(payload:{name:string;targetAmount?:number;targetDate?:string;description?:string})=>request<Vault>('/api/v1/vaults',{method:'POST',body:JSON.stringify({...payload,currency:'XOF'})},true),deposit:(id:string,amount:number)=>request<Vault>(`/api/v1/vaults/${id}/deposit`,{method:'POST',body:JSON.stringify({amount})},true)};
export const transactionApi={
  history:()=>request<{content:Transaction[]}>('/api/v1/transactions?size=100',{},true),
  quote:(amount:number)=>request<FeeQuote>('/api/v1/transactions/quote',{method:'POST',body:JSON.stringify({type:'P2P_TRANSFER',currency:'XOF',amount})},true),
  transfer:(payload:{amount:number;recipientPhone:string;description?:string})=>request<Transaction>('/api/v1/transactions/transfer',{method:'POST',body:JSON.stringify({...payload,currency:'XOF'})},true),
  lookupRecipient:(phone:string)=>request<TransferRecipientLookup>(`/api/v1/transactions/recipient?phone=${encodeURIComponent(phone)}`,{},true),
  payBill:(payload:{amount:number;billerReference:string;description?:string})=>request<Transaction>('/api/v1/transactions/bill-payment',{method:'POST',headers:{'Idempotency-Key':`${Date.now()}-${Math.random().toString(36).slice(2)}`},body:JSON.stringify({...payload,currency:'XOF'})},true),
};
export const creditApi={eligibility:()=>request<CreditEligibility>('/api/v1/credit/eligibility?currency=XOF',{},true),loans:()=>request<Loan[]>('/api/v1/credit/loans',{},true)};
export const assistantApi={
  conversations:()=>request<AssistantConversation[]>('/api/v1/assistant/conversations',{},true),
  conversation:(id:string)=>request<AssistantConversationDetail>(`/api/v1/assistant/conversations/${id}`,{},true),
  ask:(message:string,conversationId?:string)=>request<AssistantReply>('/api/v1/assistant/messages',{method:'POST',body:JSON.stringify({conversationId:conversationId||null,message})},true),
  remove:(id:string)=>request<void>(`/api/v1/assistant/conversations/${id}`,{method:'DELETE'},true),
};
export const kycApi={status:()=>request<KycStatus>('/api/v1/kyc/status',{},true),upload:(type:KycDocument['type'],file:{uri:string;name:string;mimeType?:string})=>{const body=new FormData();body.append('type',type);body.append('file',{uri:file.uri,name:file.name,type:file.mimeType||'application/octet-stream'} as unknown as Blob);return request<KycDocument>('/api/v1/kyc/documents',{method:'POST',body},true);}};
export const schedulingApi={
  list:()=>request<ScheduledTask[]>('/api/v1/scheduling/tasks/me',{},true),
  billers:()=>request<Biller[]>('/api/v1/scheduling/tasks/billers',{},true),
  create:(payload:{userId?:string;type:string;frequency:string;amount:number;currency:string;beneficiaryReference:string;description?:string;firstRunAt:string;endDate?:string;maxOccurrences?:number;fundingVaultId?:string;biller?:string;dayOfMonth?:number})=>request<ScheduledTask>('/api/v1/scheduling/tasks',{method:'POST',body:JSON.stringify(payload)},true),
  pause:(id:string)=>request<ScheduledTask>(`/api/v1/scheduling/tasks/${id}/pause`,{method:'PATCH'},true),
  resume:(id:string)=>request<ScheduledTask>(`/api/v1/scheduling/tasks/${id}/resume`,{method:'PATCH'},true),
  cancel:(id:string)=>request<ScheduledTask>(`/api/v1/scheduling/tasks/${id}/cancel`,{method:'PATCH'},true),
};

export {API_URL};
import AsyncStorage from '@react-native-async-storage/async-storage';
