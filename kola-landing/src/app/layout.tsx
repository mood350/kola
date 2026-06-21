import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
    title: "KOLA | Finance invisible.",
    description: "L'argent, en toute simplicité.",
};

export default function RootLayout({
                                       children,
                                   }: Readonly<{
    children: React.ReactNode;
}>) {
    return (
        <html lang="fr" className="antialiased">
        <body className="bg-[#0A0A0F] text-white">
        {children}
        </body>
        </html>
    );
}