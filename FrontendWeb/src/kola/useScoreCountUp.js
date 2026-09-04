import { useEffect, useState } from 'react';

export function useScoreCountUp(target = 78, step = 2, intervalMs = 22) {
  const [score, setScore] = useState(0);
  useEffect(() => {
    let v = 0;
    const timer = setInterval(() => {
      v += step;
      if (v >= target) {
        v = target;
        clearInterval(timer);
      }
      setScore(v);
    }, intervalMs);
    return () => clearInterval(timer);
  }, [target, step, intervalMs]);
  return score;
}

export function gaugeDashoffset(score, circumference = 339.3) {
  return (circumference * (1 - score / 100)).toFixed(1) + 'px';
}
