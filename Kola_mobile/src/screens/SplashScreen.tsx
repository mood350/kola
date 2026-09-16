import React, { useEffect, useRef } from 'react';
import { Animated, Easing, StyleSheet, Text, View } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import Logo from '../components/Logo';
import { c } from '../theme';

export default function SplashScreen(){
  const logoScale=useRef(new Animated.Value(.55)).current;
  const logoOpacity=useRef(new Animated.Value(0)).current;
  const titleOpacity=useRef(new Animated.Value(0)).current;
  const titleY=useRef(new Animated.Value(18)).current;
  const transfer=useRef(new Animated.Value(0)).current;

  useEffect(()=>{
    Animated.parallel([
      Animated.sequence([
        Animated.parallel([
          Animated.timing(logoOpacity,{toValue:1,duration:500,useNativeDriver:true}),
          Animated.spring(logoScale,{toValue:1,tension:55,friction:7,useNativeDriver:true}),
        ]),
        Animated.parallel([
          Animated.timing(titleOpacity,{toValue:1,duration:450,useNativeDriver:true}),
          Animated.timing(titleY,{toValue:0,duration:450,easing:Easing.out(Easing.cubic),useNativeDriver:true}),
        ]),
      ]),
      Animated.loop(Animated.sequence([
        Animated.timing(transfer,{toValue:1,duration:1100,easing:Easing.inOut(Easing.cubic),useNativeDriver:true}),
        Animated.delay(250),
        Animated.timing(transfer,{toValue:0,duration:0,useNativeDriver:true}),
        Animated.delay(350),
      ])),
    ]).start();
  },[logoOpacity,logoScale,titleOpacity,titleY,transfer]);

  const coinX=transfer.interpolate({inputRange:[0,1],outputRange:[-54,54]});
  const coinOpacity=transfer.interpolate({inputRange:[0,.08,.9,1],outputRange:[0,1,1,0]});
  return <View style={s.container}>
    <View style={s.center}>
      <Animated.View style={{opacity:logoOpacity,transform:[{scale:logoScale}]}}><Logo width={150} style={s.logo}/></Animated.View>
      <Animated.View style={{opacity:titleOpacity,transform:[{translateY:titleY}]}}>
        <Text style={s.title}>KOLA</Text>
        <Text style={s.tagline}>Votre finance. Votre avenir.</Text>
      </Animated.View>
      <Animated.View style={[s.transferCard,{opacity:titleOpacity}]}>
        <View style={s.endpoint}><Ionicons name="wallet" size={22} color={c.yellow}/></View>
        <View style={s.transferLine}><Ionicons name="arrow-forward" size={18} color="#ADC6FF"/><Animated.View style={[s.coin,{opacity:coinOpacity,transform:[{translateX:coinX}]}]}><Text style={s.coinText}>F</Text></Animated.View></View>
        <View style={s.endpoint}><Ionicons name="person" size={22} color={c.white}/></View>
        <Text style={s.transferLabel}>ENVOI INSTANTANÉ</Text>
      </Animated.View>
    </View>
    <Animated.View style={[s.loading,{opacity:titleOpacity}]}><View style={s.loadingFill}/></Animated.View>
  </View>;
}

const s=StyleSheet.create({
  container:{flex:1,backgroundColor:c.primary,alignItems:'center',justifyContent:'center'},
  center:{width:300,height:390,alignItems:'center',justifyContent:'center'},
  logo:{height:150},
  title:{fontSize:34,fontWeight:'900',letterSpacing:5,color:c.white,textAlign:'center',marginTop:5},
  tagline:{fontSize:11,letterSpacing:.8,color:'#D8E2FF',textAlign:'center',marginTop:7},
  transferCard:{width:240,height:72,borderRadius:22,backgroundColor:'#FFFFFF12',borderWidth:1,borderColor:'#FFFFFF20',marginTop:28,paddingHorizontal:16,flexDirection:'row',alignItems:'center',justifyContent:'space-between'},
  endpoint:{width:42,height:42,borderRadius:13,backgroundColor:'#FFFFFF16',alignItems:'center',justifyContent:'center'},
  transferLine:{width:104,height:42,alignItems:'center',justifyContent:'center'},
  coin:{position:'absolute',width:28,height:28,borderRadius:14,backgroundColor:c.yellow,alignItems:'center',justifyContent:'center',shadowColor:c.yellow,shadowOpacity:.7,shadowRadius:8,elevation:7},
  coinText:{fontSize:13,fontWeight:'900',color:c.primary},
  transferLabel:{position:'absolute',bottom:-18,left:0,right:0,fontSize:8,fontWeight:'800',letterSpacing:1.5,color:'#ADC6FF',textAlign:'center'},
  loading:{position:'absolute',bottom:55,width:84,height:3,borderRadius:2,backgroundColor:'#FFFFFF22',overflow:'hidden'},
  loadingFill:{width:'72%',height:'100%',borderRadius:2,backgroundColor:c.yellow},
});
