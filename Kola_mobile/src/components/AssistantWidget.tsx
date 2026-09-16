import { Ionicons } from '@expo/vector-icons';
import React, { useEffect, useRef, useState } from 'react';
import { ActivityIndicator, KeyboardAvoidingView, Modal, Platform, ScrollView, StyleSheet, Text, TextInput, TouchableOpacity, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { ApiError, AssistantMessage, assistantApi } from '../services/api';
import { c, shadow } from '../theme';

const welcome:AssistantMessage={id:'welcome',role:'ASSISTANT',content:'Bonjour ! Je suis l’assistant KOLA. Posez-moi une question sur votre compte, Bankivi, vos prêts ou l’utilisation de l’application.',createdAt:new Date().toISOString()};

export default function AssistantWidget(){
  const insets=useSafeAreaInsets(),scroll=useRef<ScrollView>(null);
  const [open,setOpen]=useState(false),[loading,setLoading]=useState(true),[sending,setSending]=useState(false),[unavailable,setUnavailable]=useState(false);
  const [conversationId,setConversationId]=useState<string>(),[messages,setMessages]=useState<AssistantMessage[]>([welcome]),[input,setInput]=useState(''),[remaining,setRemaining]=useState<number>(),[error,setError]=useState('');

  useEffect(()=>{let active=true;assistantApi.conversations().then(async conversations=>{if(!active)return;if(conversations[0]){const detail=await assistantApi.conversation(conversations[0].id);if(active){setConversationId(detail.id);setMessages(detail.messages.length?detail.messages:[welcome]);}}}).catch(value=>{if(active&&value instanceof ApiError&&value.status===503){setUnavailable(true);setError('L’assistant KOLA est temporairement indisponible. Vous pourrez réessayer dans un instant.');}}).finally(()=>{if(active)setLoading(false);});return()=>{active=false};},[]);
  useEffect(()=>{if(open)setTimeout(()=>scroll.current?.scrollToEnd({animated:true}),80);},[open,messages,sending]);

  const newConversation=()=>{setConversationId(undefined);setMessages([welcome]);setInput('');setError('');setRemaining(undefined);};
  const send=async()=>{const question=input.trim();if(!question||sending)return;const optimistic:AssistantMessage={id:`local-${Date.now()}`,role:'USER',content:question,createdAt:new Date().toISOString()};setMessages(value=>[...value,optimistic]);setInput('');setError('');setUnavailable(false);setSending(true);try{const reply=await assistantApi.ask(question,conversationId);setConversationId(reply.conversationId);setRemaining(reply.remainingToday);setMessages(value=>[...value,reply.answer]);}catch(value){const apiError=value instanceof ApiError?value:null;setMessages(current=>current.filter(message=>message.id!==optimistic.id));setInput(question);if(apiError?.status===503)setUnavailable(true);setError(apiError?.status===429?'Vous avez atteint votre quota de messages pour aujourd’hui.':apiError?.status===503?'L’assistant KOLA est temporairement indisponible. Votre question est conservée, vous pouvez réessayer.':apiError?.message||'L’assistant ne répond pas pour le moment.');}finally{setSending(false);}};

  return <>
    <TouchableOpacity accessibilityLabel="Ouvrir l’assistant KOLA" onPress={()=>setOpen(true)} style={[s.fab,{bottom:Math.max(insets.bottom,10)+78}]}>
      {loading?<ActivityIndicator color={c.primary}/>:<Ionicons name="sparkles" size={25} color={c.primary}/>}<View style={[s.online,unavailable&&s.offline]}/>
    </TouchableOpacity>
    <Modal visible={open} transparent animationType="fade" onRequestClose={()=>setOpen(false)}>
      <KeyboardAvoidingView behavior={Platform.OS==='ios'?'padding':undefined} style={s.overlay}>
        <View style={[s.panel,{paddingBottom:Math.max(insets.bottom,12)}]}>
          <View style={s.header}><View style={s.bot}><Ionicons name="sparkles" size={21} color={c.primary}/></View><View style={s.headerCopy}><Text style={s.title}>Assistant KOLA</Text><Text style={[s.status,unavailable&&s.statusOffline]}>{unavailable?'Temporairement indisponible':'En ligne · Conseils personnalisés'}</Text></View><TouchableOpacity accessibilityLabel="Nouvelle conversation" onPress={newConversation} style={s.headerButton}><Ionicons name="add" size={22} color={c.primary}/></TouchableOpacity><TouchableOpacity accessibilityLabel="Fermer" onPress={()=>setOpen(false)} style={s.headerButton}><Ionicons name="close" size={22} color={c.ink}/></TouchableOpacity></View>
          <ScrollView ref={scroll} style={s.chat} contentContainerStyle={s.chatContent} keyboardShouldPersistTaps="handled">
            {messages.map(message=><View key={message.id} style={[s.bubble,message.role==='USER'?s.userBubble:s.assistantBubble]}><Text style={[s.message,message.role==='USER'&&s.userMessage]}>{message.content}</Text></View>)}
            {sending&&<View style={[s.bubble,s.assistantBubble,s.typing]}><ActivityIndicator size="small" color={c.primary}/><Text style={s.typingText}>KOLA réfléchit…</Text></View>}
            {!!error&&<View style={s.error}><Ionicons name="alert-circle-outline" size={17} color={c.red}/><Text style={s.errorText}>{error}</Text></View>}
          </ScrollView>
          {remaining!==undefined&&remaining<=5&&<Text style={s.quota}>{remaining} message{remaining>1?'s':''} restant{remaining>1?'s':''} aujourd’hui</Text>}
          <View style={s.composer}><TextInput value={input} onChangeText={setInput} onSubmitEditing={send} editable={!sending} returnKeyType="send" placeholder="Écrivez votre question…" placeholderTextColor={c.muted} style={s.input} multiline maxLength={1000}/><TouchableOpacity disabled={!input.trim()||sending} onPress={send} style={[s.send,(!input.trim()||sending)&&s.sendDisabled]}><Ionicons name="arrow-up" size={20} color={c.white}/></TouchableOpacity></View>
          <Text style={s.notice}>Ne partagez jamais votre code PIN ou votre OTP.</Text>
        </View>
      </KeyboardAvoidingView>
    </Modal>
  </>;
}

const s=StyleSheet.create({fab:{position:'absolute',right:19,width:56,height:56,borderRadius:28,backgroundColor:c.yellow,alignItems:'center',justifyContent:'center',zIndex:40,...shadow,shadowOpacity:.28,elevation:16},online:{position:'absolute',right:2,bottom:3,width:13,height:13,borderRadius:7,backgroundColor:c.green,borderWidth:2,borderColor:c.white},offline:{backgroundColor:c.red},overlay:{flex:1,backgroundColor:'#071B5266',justifyContent:'flex-end'},panel:{height:'82%',backgroundColor:c.bg,borderTopLeftRadius:26,borderTopRightRadius:26,overflow:'hidden'},header:{height:72,paddingHorizontal:17,flexDirection:'row',alignItems:'center',gap:10,backgroundColor:c.white,borderBottomWidth:1,borderBottomColor:c.border},bot:{width:43,height:43,borderRadius:22,backgroundColor:c.yellow,alignItems:'center',justifyContent:'center'},headerCopy:{flex:1},title:{fontSize:16,fontWeight:'900',color:c.ink},status:{fontSize:9,color:c.greenDark,marginTop:2},statusOffline:{color:c.red},headerButton:{width:37,height:37,borderRadius:19,backgroundColor:c.pale,alignItems:'center',justifyContent:'center'},chat:{flex:1},chatContent:{padding:16,gap:10},bubble:{maxWidth:'84%',borderRadius:18,paddingHorizontal:14,paddingVertical:11},assistantBubble:{alignSelf:'flex-start',backgroundColor:c.white,borderBottomLeftRadius:5,borderWidth:1,borderColor:c.border},userBubble:{alignSelf:'flex-end',backgroundColor:c.primary,borderBottomRightRadius:5},message:{fontSize:13,lineHeight:19,color:c.ink},userMessage:{color:c.white},typing:{flexDirection:'row',alignItems:'center',gap:8},typingText:{fontSize:11,color:c.muted},error:{marginTop:3,padding:10,borderRadius:12,backgroundColor:'#FFF0F0',flexDirection:'row',alignItems:'center',gap:7},errorText:{fontSize:10,lineHeight:14,color:c.red,flex:1},quota:{fontSize:9,color:c.yellowDark,textAlign:'center',marginBottom:5},composer:{marginHorizontal:14,minHeight:52,maxHeight:110,borderRadius:26,backgroundColor:c.white,borderWidth:1,borderColor:c.border,paddingLeft:16,paddingRight:5,flexDirection:'row',alignItems:'center'},input:{flex:1,fontSize:13,color:c.ink,maxHeight:90,paddingVertical:11},send:{width:42,height:42,borderRadius:21,backgroundColor:c.primary,alignItems:'center',justifyContent:'center'},sendDisabled:{opacity:.35},notice:{fontSize:8,color:c.muted,textAlign:'center',marginTop:7}});
