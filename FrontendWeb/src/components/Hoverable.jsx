import { useState } from 'react';
import { expandBorder } from '../lib/style';

// Mirrors the mockup's `style` + `style-hover` pairing: swaps in extra style
// rules while the pointer is over the element.
export default function Hoverable({ as: Tag = 'div', style, hoverStyle, children, ...rest }) {
  const [hover, setHover] = useState(false);
  const base = hoverStyle ? expandBorder(style) : style;
  return (
    <Tag
      style={hover && hoverStyle ? { ...base, ...hoverStyle } : base}
      onMouseEnter={(e) => {
        setHover(true);
        rest.onMouseEnter?.(e);
      }}
      onMouseLeave={(e) => {
        setHover(false);
        rest.onMouseLeave?.(e);
      }}
      {...rest}
    >
      {children}
    </Tag>
  );
}
