const API_URL=(process.env.EXPO_PUBLIC_API_URL||'http://10.0.2.2:8080').replace(/\/$/,'');

type ApiEnvelope<T>={success:boolean;message?:string;data:T};
type ApiErrorBody={message?:string;fieldErrors?:Record<string,string>};

export type DogaaUser={
  id:string;firstName:string;lastName:string;phone:string;email?:string;
  kycTier:string;phoneVerified:boolean;emailVerified:boolean;
};
export type AuthSession={accessToken:string;refreshToken:string;tokenType:string;expiresIn:number;user:DogaaUser};

export class ApiError extends Error{
  constructor(message:string,public status:number,public fieldErrors?:Record<string,string>){super(message);}
}

async function request<T>(path:string,options:RequestInit={}):Promise<T>{
  let response:Response;
  try{
    response=await fetch(`${API_URL}${path}`,{...options,headers:{Accept:'application/json','Content-Type':'application/json',...options.headers}});
  }catch{
    throw new ApiError(`Serveur DOGAA inaccessible (${API_URL}). Vérifiez l'adresse EXPO_PUBLIC_API_URL.`,0);
  }
  const body=await response.json().catch(()=>({})) as ApiEnvelope<T>&ApiErrorBody;
  if(!response.ok)throw new ApiError(body.message||'Une erreur est survenue.',response.status,body.fieldErrors);
  return body.data;
}

export const authApi={
  requestOtp:(phone:string)=>request<{phone:string;codeExpiresInSeconds:number;resendAvailableAt:string}>('/api/v1/auth/register/request-otp',{method:'POST',body:JSON.stringify({phone})}),
  verifyOtp:(phone:string,code:string)=>request<{verificationToken:string;expiresInSeconds:number}>('/api/v1/auth/register/verify-otp',{method:'POST',body:JSON.stringify({phone,code})}),
  register:(payload:{verificationToken:string;firstName:string;lastName:string;email?:string;dateOfBirth:string;pin:string;confirmPin:string;acceptedPrivacyPolicy:boolean})=>request<AuthSession>('/api/v1/auth/register',{method:'POST',body:JSON.stringify({...payload,country:'TG'})}),
  login:(phone:string,pin:string)=>request<AuthSession>('/api/v1/auth/login',{method:'POST',body:JSON.stringify({phone,pin})}),
  logout:(refreshToken:string)=>request<void>('/api/v1/auth/logout',{method:'POST',body:JSON.stringify({refreshToken})}),
};

export {API_URL};
