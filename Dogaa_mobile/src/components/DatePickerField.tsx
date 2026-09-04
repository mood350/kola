import DateTimePicker, { DateTimePickerEvent } from '@react-native-community/datetimepicker';
import { Ionicons } from '@expo/vector-icons';
import React, { useState } from 'react';
import { Platform, StyleSheet, Text, TouchableOpacity, View } from 'react-native';
import { c } from '../theme';

const dateValue=(value:string)=>{const parsed=value?new Date(`${value}T12:00:00`):new Date();return Number.isNaN(parsed.getTime())?new Date():parsed;};
const dateText=(date:Date)=>`${date.getFullYear()}-${String(date.getMonth()+1).padStart(2,'0')}-${String(date.getDate()).padStart(2,'0')}`;
const timeValue=(value:string)=>{const [hours,minutes]=(value||'12:00').split(':').map(Number);const date=new Date();date.setHours(hours||0,minutes||0,0,0);return date;};
const timeText=(date:Date)=>`${String(date.getHours()).padStart(2,'0')}:${String(date.getMinutes()).padStart(2,'0')}`;

export default function DatePickerField({value,onChange,mode='date',placeholder,minimumDate,maximumDate}:{value:string;onChange:(value:string)=>void;mode?:'date'|'time';placeholder:string;minimumDate?:Date;maximumDate?:Date}){
  const [visible,setVisible]=useState(false);
  const selected=mode==='date'?dateValue(value):timeValue(value);
  const displayedValue=mode==='time'&&value?value.replace(':',' h '):value;
  const change=(event:DateTimePickerEvent,next?:Date)=>{if(Platform.OS==='android')setVisible(false);if(event.type==='set'&&next)onChange(mode==='date'?dateText(next):timeText(next));};
  return <View><TouchableOpacity onPress={()=>setVisible(true)} style={s.field}><Ionicons name={mode==='date'?'calendar-outline':'time-outline'} size={19} color={c.primary}/><Text style={[s.value,!value&&s.placeholder]}>{displayedValue||placeholder}</Text><Ionicons name="create-outline" size={17} color={c.muted}/></TouchableOpacity>{visible&&<DateTimePicker value={selected} mode={mode} display={Platform.OS==='ios'?'spinner':mode==='time'?'clock':'calendar'} is24Hour locale="fr-FR" minimumDate={minimumDate} maximumDate={maximumDate} onChange={change}/>} {visible&&Platform.OS==='ios'&&<TouchableOpacity onPress={()=>setVisible(false)} style={s.done}><Text style={s.doneText}>Terminé</Text></TouchableOpacity>}</View>;
}

const s=StyleSheet.create({field:{height:48,borderRadius:11,borderWidth:1,borderColor:c.border,paddingHorizontal:12,backgroundColor:c.white,flexDirection:'row',alignItems:'center',gap:9},value:{fontSize:12,color:c.ink,flex:1},placeholder:{color:c.muted},done:{height:36,alignItems:'flex-end',justifyContent:'center'},doneText:{fontSize:11,fontWeight:'800',color:c.primary}});
