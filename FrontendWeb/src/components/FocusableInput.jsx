import { useState } from 'react';
import { expandBorder } from '../lib/style';

// Mirrors the mockup's `style` + `style-focus` pairing on text inputs.
export default function FocusableInput({ style, focusStyle, ...rest }) {
  const [focus, setFocus] = useState(false);
  const base = focusStyle ? expandBorder(style) : style;
  return (
    <input
      style={focus && focusStyle ? { ...base, ...focusStyle } : base}
      onFocus={(e) => {
        setFocus(true);
        rest.onFocus?.(e);
      }}
      onBlur={(e) => {
        setFocus(false);
        rest.onBlur?.(e);
      }}
      {...rest}
    />
  );
}
