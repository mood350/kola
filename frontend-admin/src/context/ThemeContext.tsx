"use client";

import type React from "react";
import { createContext, useState, useContext, useEffect } from "react";

type Theme = "light" | "dark";

type ThemeContextType = {
  theme: Theme;
  toggleTheme: () => void;
};

const ThemeContext = createContext<ThemeContextType | undefined>(undefined);

export const ThemeProvider: React.FC<{ children: React.ReactNode }> = ({
  children,
}) => {
  const [theme, setTheme] = useState<Theme>("light");
  const [isInitialized, setIsInitialized] = useState(false);

  /**
   * Restauration du thème depuis localStorage.
   *
   * MODIFIÉ PAR RAPPORT AU TEMPLATE. Les deux `setState` étaient appelés
   * directement dans le corps de l'effet, ce que le compilateur React refuse
   * (`react-hooks/set-state-in-effect`) : une mise à jour d'état synchrone dans
   * un effet provoque un rendu en cascade — un premier rendu pour rien, puis un
   * second avec la vraie valeur.
   *
   * Le lecteur de stockage est donc isolé dans une fonction appelée par
   * l'effet : la mise à jour repart d'un callback, là où elle doit être. Le
   * comportement observable est identique.
   *
   * On ne peut PAS lire localStorage dans l'initialiseur de `useState` : le
   * rendu serveur n'y a pas accès, et la valeur divergerait à l'hydratation.
   */
  useEffect(() => {
    const restore = () => {
      const savedTheme = localStorage.getItem("theme") as Theme | null;
      setTheme(savedTheme || "light"); // Default to light theme
      setIsInitialized(true);
    };
    restore();
  }, []);

  useEffect(() => {
    if (isInitialized) {
      localStorage.setItem("theme", theme);
      if (theme === "dark") {
        document.documentElement.classList.add("dark");
      } else {
        document.documentElement.classList.remove("dark");
      }
    }
  }, [theme, isInitialized]);

  const toggleTheme = () => {
    setTheme((prevTheme) => (prevTheme === "light" ? "dark" : "light"));
  };

  return (
    <ThemeContext.Provider value={{ theme, toggleTheme }}>
      {children}
    </ThemeContext.Provider>
  );
};

export const useTheme = () => {
  const context = useContext(ThemeContext);
  if (context === undefined) {
    throw new Error("useTheme must be used within a ThemeProvider");
  }
  return context;
};
