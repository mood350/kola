import { useState } from 'react';

// Mirrors the mockup's `style` + `style-focus` pairing on text inputs.
export default function FocusableInput({ style, focusStyle, ...rest }) {
  const [focus, setFocus] = useState(false);
  return (
    <input
      style={focus && focusStyle ? { ...style, ...focusStyle } : style}
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
