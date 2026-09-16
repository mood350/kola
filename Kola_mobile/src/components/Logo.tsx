import React from 'react';
import { Image, ImageStyle, StyleProp } from 'react-native';

const logo = require('../../assets/kola-logo-transparent.png');

export default function Logo({width=120,style}:{width?:number;style?:StyleProp<ImageStyle>}){
  return <Image source={logo} style={[{width,height:width*.34},style]} resizeMode="contain" accessibilityLabel="Logo KOLA"/>;
}
