import React, { useEffect, useRef } from 'react';
import { Animated, Easing, StyleSheet, Text, View } from 'react-native';
import Logo from '../components/Logo';
import { c } from '../theme';

export default function SplashScreen(){
  const logoScale=useRef(new Animated.Value(.55)).current;
  const logoOpacity=useRef(new Animated.Value(0)).current;
  const titleOpacity=useRef(new Animated.Value(0)).current;
  const titleY=useRef(new Animated.Value(18)).current;
  const glowScale=useRef(new Animated.Value(.7)).current;
  const orbit=useRef(new Animated.Value(0)).current;

  useEffect(()=>{
    Animated.parallel([
      Animated.sequence([
        Animated.parallel([
          Animated.timing(logoOpacity,{toValue:1,duration:500,useNativeDriver:true}),
          Animated.spring(logoScale,{toValue:1,tension:55,friction:7,useNativeDriver:true}),
          Animated.timing(glowScale,{toValue:1,duration:850,easing:Easing.out(Easing.cubic),useNativeDriver:true}),
        ]),
        Animated.parallel([
          Animated.timing(titleOpacity,{toValue:1,duration:450,useNativeDriver:true}),
          Animated.timing(titleY,{toValue:0,duration:450,easing:Easing.out(Easing.cubic),useNativeDriver:true}),
        ]),
      ]),
      Animated.loop(Animated.timing(orbit,{toValue:1,duration:2600,easing:Easing.linear,useNativeDriver:true})),
    ]).start();
  },[glowScale,logoOpacity,logoScale,orbit,titleOpacity,titleY]);

  const rotation=orbit.interpolate({inputRange:[0,1],outputRange:['0deg','360deg']});
  return <View style={s.container}>
    <View style={s.center}>
      <Animated.View style={[s.glow,{transform:[{scale:glowScale}]}]}/>
      <Animated.View style={[s.orbit,{transform:[{rotate:rotation}]}]}><View style={s.spark}/></Animated.View>
      <Animated.View style={{opacity:logoOpacity,transform:[{scale:logoScale}]}}><Logo width={150} style={s.logo}/></Animated.View>
      <Animated.View style={{opacity:titleOpacity,transform:[{translateY:titleY}]}}>
        <Text style={s.title}>DOGAA</Text>
        <Text style={s.tagline}>Votre finance. Votre avenir.</Text>
      </Animated.View>
    </View>
    <Animated.View style={[s.loading,{opacity:titleOpacity}]}><View style={s.loadingFill}/></Animated.View>
  </View>;
}

const s=StyleSheet.create({
  container:{flex:1,backgroundColor:c.primary,alignItems:'center',justifyContent:'center'},
  center:{width:280,height:310,alignItems:'center',justifyContent:'center'},
  glow:{position:'absolute',top:34,width:220,height:220,borderRadius:110,backgroundColor:'#164C91',opacity:.52},
  orbit:{position:'absolute',top:25,width:238,height:238,borderRadius:119,borderWidth:1,borderColor:'#FFFFFF22'},
  spark:{position:'absolute',top:15,right:21,width:12,height:12,borderRadius:6,backgroundColor:c.yellow,shadowColor:c.yellow,shadowOpacity:.9,shadowRadius:10,elevation:8},
  logo:{height:150},
  title:{fontSize:34,fontWeight:'900',letterSpacing:5,color:c.white,textAlign:'center',marginTop:5},
  tagline:{fontSize:11,letterSpacing:.8,color:'#D8E2FF',textAlign:'center',marginTop:7},
  loading:{position:'absolute',bottom:55,width:84,height:3,borderRadius:2,backgroundColor:'#FFFFFF22',overflow:'hidden'},
  loadingFill:{width:'72%',height:'100%',borderRadius:2,backgroundColor:c.yellow},
});
