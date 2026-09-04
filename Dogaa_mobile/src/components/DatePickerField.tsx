import DateTimePicker, { DateTimePickerChangeEvent } from '@react-native-community/datetimepicker';
import { Ionicons } from '@expo/vector-icons';
import React, { useState } from 'react';
import { Modal, Platform, StyleSheet, Text, TouchableOpacity, View } from 'react-native';
import { c } from '../theme';

const dateValue=(value:string)=>{const parsed=value?new Date(`${value}T12:00:00`):new Date();return Number.isNaN(parsed.getTime())?new Date():parsed;};
const dateText=(date:Date)=>`${date.getFullYear()}-${String(date.getMonth()+1).padStart(2,'0')}-${String(date.getDate()).padStart(2,'0')}`;
const timeValue=(value:string)=>{const [hours,minutes]=(value||'12:00').split(':').map(Number);const date=new Date();date.setHours(hours||0,minutes||0,0,0);return date;};
const timeText=(date:Date)=>`${String(date.getHours()).padStart(2,'0')}:${String(date.getMinutes()).padStart(2,'0')}`;

export default function DatePickerField({value,onChange,mode='date',placeholder,minimumDate,maximumDate}:{value:string;onChange:(value:string)=>void;mode?:'date'|'time';placeholder:string;minimumDate?:Date;maximumDate?:Date}){
  const [visible,setVisible]=useState(false);
  const selected=mode==='date'?dateValue(value):timeValue(value);
  const displayedValue=mode==='time'&&value?value.replace(':',' h '):value;
  const change=(_event:DateTimePickerChangeEvent,next:Date)=>{onChange(mode==='date'?dateText(next):timeText(next));if(Platform.OS==='android')setVisible(false);};
  const pickerProps={value:selected,mode,display:Platform.OS==='ios'?'spinner' as const:mode==='time'?'clock' as const:'calendar' as const,is24Hour:true,locale:'fr-FR',minimumDate,maximumDate,onValueChange:change,onDismiss:()=>setVisible(false)};
  return <View>
    <TouchableOpacity onPress={()=>setVisible(true)} style={s.field}>
      <Ionicons name={mode==='date'?'calendar-outline':'time-outline'} size={19} color={c.primary}/>
      <Text style={[s.value,!value&&s.placeholder]}>{displayedValue||placeholder}</Text>
      <Ionicons name="create-outline" size={17} color={c.muted}/>
    </TouchableOpacity>
    {visible&&Platform.OS==='android'?<DateTimePicker {...pickerProps} style={s.picker}/>:null}
    {Platform.OS==='ios'?<Modal transparent visible={visible} animationType="fade" onRequestClose={()=>setVisible(false)}>
      <TouchableOpacity activeOpacity={1} onPress={()=>setVisible(false)} style={s.overlay}>
        <TouchableOpacity activeOpacity={1} style={s.sheet}>
          <View style={s.sheetHead}>
            <Text style={s.sheetTitle}>{mode==='time'?'Choisir l’heure':'Choisir la date'}</Text>
            <TouchableOpacity onPress={()=>setVisible(false)} style={s.done}><Text style={s.doneText}>Terminé</Text></TouchableOpacity>
          </View>
          <DateTimePicker {...pickerProps} style={s.picker}/>
        </TouchableOpacity>
      </TouchableOpacity>
    </Modal>:null}
  </View>;
}

const s=StyleSheet.create({field:{height:48,borderRadius:11,borderWidth:1,borderColor:c.border,paddingHorizontal:12,backgroundColor:c.white,flexDirection:'row',alignItems:'center',gap:9},value:{fontSize:12,color:c.ink,flex:1},placeholder:{color:c.muted},overlay:{flex:1,backgroundColor:'#00235366',justifyContent:'flex-end'},sheet:{width:'100%',backgroundColor:c.white,borderTopLeftRadius:24,borderTopRightRadius:24,paddingHorizontal:20,paddingTop:12,paddingBottom:28},sheetHead:{height:42,flexDirection:'row',alignItems:'center',justifyContent:'space-between'},sheetTitle:{fontSize:16,fontWeight:'800',color:c.primary},picker:{width:'100%'},done:{minWidth:70,height:36,alignItems:'flex-end',justifyContent:'center'},doneText:{fontSize:12,fontWeight:'800',color:c.primary}});
