"use client";

import { motion } from "framer-motion";
import {
    ArrowRight, Shield, Clock, QrCode, UserPlus, CreditCard,
    FileCheck, Search, MapPin, Building, CheckCircle2, Send, Lock
} from "lucide-react";

const fadeUp = {
    hidden: { opacity: 0, y: 30 },
    visible: { opacity: 1, y: 0, transition: { duration: 0.6, ease: [0.22, 1, 0.36, 1] } },
};

export default function Home() {
    return (
        <div className="min-h-screen bg-[#0A0A0F] text-white overflow-x-hidden relative">

            {/* AMBIANCE */}
            <div className="fixed top-0 left-0 w-full h-full pointer-events-none z-0">
                <div className="absolute top-[-20%] left-[-10%] w-[600px] h-[600px] bg-blue-600/10 rounded-full blur-[150px]"></div>
                <div className="absolute bottom-[-20%] right-[-10%] w-[600px] h-[600px] bg-purple-600/10 rounded-full blur-[150px]"></div>
            </div>

            {/* ═══════ NAVBAR ═══════ */}
            <nav className="fixed top-0 w-full z-50 bg-[#0A0A0F]/60 backdrop-blur-2xl border-b border-white/5">
                <div className="max-w-6xl mx-auto px-6 h-16 flex items-center justify-between">
                    <div className="text-xl font-bold tracking-widest text-white/90">KOLA</div>
                    <div className="hidden md:flex items-center gap-8 text-sm font-medium text-white/40">
                        <a href="#services" className="hover:text-white/90 transition">Services</a>
                        <a href="#comment" className="hover:text-white/90 transition">Comment ça marche</a>
                        <a href="#pourquoi" className="hover:text-white/90 transition">Pourquoi nous</a>
                    </div>
                    <div className="flex gap-3">
                        <button className="text-sm font-medium text-white/60 hover:text-white transition px-4 py-2">Se connecter</button>
                        <button className="bg-white text-[#0A0A0F] text-sm font-bold px-5 py-2.5 rounded-full hover:bg-gray-100 transition-colors">
                            Créer un compte
                        </button>
                    </div>
                </div>
            </nav>

            {/* ═══════ HERO (Style PNGDAT : Texte à gauche, Carte de suivi à droite) ═══════ */}
            <section className="relative z-10 pt-32 pb-24 px-6">
                <div className="max-w-6xl mx-auto grid grid-cols-1 lg:grid-cols-2 gap-16 items-center">

                    <div>
                        <motion.h1
                            initial="hidden" animate="visible" variants={fadeUp}
                            className="text-4xl md:text-5xl font-black text-white tracking-tight leading-tight mb-6"
                        >
                            Vos finances,<br />
                            <span className="text-white/30">simplifiées</span> en ligne.
                        </motion.h1>
                        <motion.p
                            initial="hidden" animate="visible" variants={{ ...fadeUp, transition: { delay: 0.1 } }}
                            className="text-white/40 text-lg font-normal mb-8 max-w-md leading-relaxed"
                        >
                            Envoyez de l'argent, créez des coffres-forts et payez vos marchands sans vous déplacer.
                        </motion.p>
                        <motion.button
                            initial="hidden" animate="visible" variants={{ ...fadeUp, transition: { delay: 0.2 } }}
                            className="group bg-white text-[#0A0A0F] px-8 py-4 rounded-full font-bold flex items-center gap-2 hover:bg-gray-100 transition-all w-fit"
                        >
                            Découvrir nos services <ArrowRight size={18} className="group-hover:translate-x-1 transition-transform" />
                        </motion.button>
                    </div>

                    {/* CARTE DE SUIVI (Inspirée de l'image 1) */}
                    <motion.div
                        initial={{ opacity: 0, x: 40 }} animate={{ opacity: 1, x: 0 }} transition={{ delay: 0.3, duration: 0.8 }}
                        className="relative bg-white/[0.03] backdrop-blur-xl rounded-3xl border border-white/10 p-6 shadow-2xl shadow-black/50"
                    >
                        <div className="absolute inset-0 bg-gradient-to-br from-white/5 to-transparent rounded-3xl pointer-events-none"></div>

                        <div className="relative z-10">
                            <div className="flex items-center justify-between mb-6">
                                <span className="text-xs font-bold text-white/30 uppercase tracking-wider">Dernière transaction</span>
                                <span className="bg-green-500/10 text-green-400 text-xs font-bold px-3 py-1 rounded-full border border-green-500/20">
                  Succès
                </span>
                            </div>

                            <h3 className="text-xl font-bold text-white mb-4">Transfert vers Kofi M.</h3>

                            <div className="space-y-4 mb-6">
                                <div className="flex justify-between text-sm">
                                    <span className="text-white/30">Référence</span>
                                    <span className="text-white/80 font-medium">KLA-2026-847291</span>
                                </div>
                                <div className="flex justify-between text-sm">
                                    <span className="text-white/30">Montant</span>
                                    <span className="text-white font-bold text-lg">20 000 XOF</span>
                                </div>
                                <div className="flex justify-between text-sm">
                                    <span className="text-white/30">Centre de traitement</span>
                                    <span className="text-white/60">KOLA - Lomé</span>
                                </div>
                                <div className="flex justify-between text-sm">
                                    <span className="text-white/30">Durée moyenne</span>
                                    <span className="text-white/60">Instantané</span>
                                </div>
                            </div>

                            <div className="border-t border-white/5 pt-4 flex items-center gap-3">
                                <div className="w-8 h-8 bg-blue-500/20 rounded-lg flex items-center justify-center">
                                    <QrCode size={16} className="text-blue-400" />
                                </div>
                                <div>
                                    <p className="text-xs text-white/30">Code de retrait</p>
                                    <p className="text-sm font-bold text-white tracking-widest">847 291</p>
                                </div>
                            </div>
                        </div>
                    </motion.div>
                </div>
            </section>

            {/* ═══════ STATS BAND (Inspiré de l'image 2) ═══════ */}
            <section className="relative z-10 py-16 px-6 border-y border-white/5">
                <div className="max-w-5xl mx-auto grid grid-cols-2 md:grid-cols-4 gap-8 text-center">
                    {[
                        { value: "10 247+", label: "Transactions traitées" },
                        { value: "4", label: "Services financiers" },
                        { value: "99.9%", label: "Satisfaction clients" },
                        { value: "6", label: "Pays couverts" }
                    ].map((stat, i) => (
                        <motion.div key={i} initial="hidden" whileInView="visible" viewport={{ once: true }} variants={{ ...fadeUp, transition: { delay: i * 0.1 } }}>
                            <div className="text-4xl font-black text-white tracking-tight">{stat.value}</div>
                            <div className="text-xs text-white/20 mt-2 font-medium">{stat.label}</div>
                        </motion.div>
                    ))}
                </div>
            </section>

            {/* ═══════ NOS SERVICES (Inspiré de l'image 2) ═══════ */}
            <section id="services" className="relative z-10 py-24 px-6">
                <div className="max-w-6xl mx-auto">
                    <motion.div initial="hidden" whileInView="visible" viewport={{ once: true }} variants={fadeUp} className="mb-16">
                        <h2 className="text-3xl md:text-4xl font-black text-white tracking-tight mb-2">Nos services financiers</h2>
                        <p className="text-white/30">Les outils dont vous avez besoin, centralisés.</p>
                    </motion.div>

                    <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                        {[
                            { icon: <Send size={24} />, title: "Transfert d'argent", desc: "Envoyez de l'argent instantanément à vos proches partout en Afrique de l'Ouest." },
                            { icon: <Lock size={24} />, title: "Épargne Coffre-fort", desc: "Verrouillez vos fonds pour atteindre vos objectifs plus rapidement." },
                            { icon: <CreditCard size={24} />, title: "Paiement Marchand", desc: "Payez vos fournisseurs et marchands directement depuis l'application." },
                            { icon: <Building size={24} />, title: "Cash-in / Cash-out", desc: "Alimentez votre portefeuille ou retirez via nos centres partenaires." }
                        ].map((item, i) => (
                            <motion.div
                                key={i} initial="hidden" whileInView="visible" viewport={{ once: true }} variants={{ ...fadeUp, transition: { delay: i * 0.1 } }}
                                className="group bg-white/[0.02] backdrop-blur-sm p-8 rounded-2xl border border-white/5 hover:border-white/10 hover:bg-white/[0.04] transition-all duration-500 flex gap-5"
                            >
                                <div className="w-12 h-12 bg-white/5 rounded-xl flex items-center justify-center text-white/40 border border-white/5 group-hover:bg-white/10 group-hover:text-white/80 transition-all flex-shrink-0">
                                    {item.icon}
                                </div>
                                <div>
                                    <h3 className="text-lg font-bold text-white mb-2">{item.title}</h3>
                                    <p className="text-white/30 text-sm leading-relaxed">{item.desc}</p>
                                </div>
                            </motion.div>
                        ))}
                    </div>
                </div>
            </section>

            {/* ═══════ COMMENT ÇA MARCHE ? (Inspiré de l'image 3 - Frise chronologique) ═══════ */}
            <section id="comment" className="relative z-10 py-24 px-6 bg-white/[0.01]">
                <div className="max-w-5xl mx-auto">
                    <motion.div initial="hidden" whileInView="visible" viewport={{ once: true }} variants={fadeUp} className="text-center mb-20">
                        <h2 className="text-3xl md:text-4xl font-black text-white tracking-tight mb-4">Comment ça marche ?</h2>
                        <p className="text-white/30 max-w-md mx-auto">Quatre étapes simples pour vos transactions.</p>
                    </motion.div>

                    <div className="grid grid-cols-1 md:grid-cols-4 gap-8 relative">
                        {/* Ligne de connexion (cachée sur mobile) */}
                        <div className="hidden md:block absolute top-12 left-[12.5%] right-[12.5%] h-[2px] bg-gradient-to-r from-blue-500/0 via-blue-500/30 to-blue-500/0"></div>

                        {[
                            { step: "01", icon: <UserPlus size={24} />, title: "Créez votre compte", desc: "Inscription gratuite avec votre numéro de téléphone et votre email." },
                            { step: "02", icon: <Search size={24} />, title: "Choisissez un service", desc: "Sélectionnez l'action que vous souhaitez effectuer (Transfert, Épargne...)." },
                            { step: "03", icon: <CreditCard size={24} />, title: "Exécutez l'action", desc: "Confirmez avec votre code PIN ou votre biométrie en toute sécurité." },
                            { step: "04", icon: <FileCheck size={24} />, title: "C'est terminé !", desc: "Recevez la confirmation instantanée. L'argent est arrivé." }
                        ].map((item, i) => (
                            <motion.div
                                key={i} initial="hidden" whileInView="visible" viewport={{ once: true }} variants={{ ...fadeUp, transition: { delay: i * 0.15 } }}
                                className="relative text-center"
                            >
                                <div className="w-24 h-24 mx-auto mb-6 bg-white/[0.03] border border-white/10 rounded-2xl flex items-center justify-center text-white/60 relative z-10">
                                    {item.icon}
                                    <span className="absolute -top-2 -right-2 w-7 h-7 bg-blue-600 rounded-full text-xs font-bold flex items-center justify-center text-white shadow-lg shadow-blue-600/30">
                    {item.step}
                  </span>
                                </div>
                                <h3 className="text-lg font-bold text-white mb-2">{item.title}</h3>
                                <p className="text-white/30 text-sm leading-relaxed">{item.desc}</p>
                            </motion.div>
                        ))}
                    </div>
                </div>
            </section>

            {/* ═══════ POURQUOI NOUS CHOISIR ? (Inspiré de l'image 4) ═══════ */}
            <section id="pourquoi" className="relative z-10 py-24 px-6">
                <div className="max-w-6xl mx-auto">
                    <motion.div initial="hidden" whileInView="visible" viewport={{ once: true }} variants={fadeUp} className="text-center mb-16">
                        <h2 className="text-3xl md:text-4xl font-black text-white tracking-tight mb-4">Pourquoi nous choisir ?</h2>
                        <p className="text-white/30 max-w-lg mx-auto">Une plateforme pensée pour les citoyens africains.</p>
                    </motion.div>

                    <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
                        {[
                            { icon: <Shield size={32} />, title: "Sécurité renforcée", desc: "Cryptage de bout en bout et authentification forte (PIN & Biométrie) pour protéger votre argent." },
                            { icon: <Clock size={32} />, title: "Gain de temps", desc: "Fini les longues files d'attente. Un transfert ne prend que quelques secondes." },
                            { icon: <MapPin size={32} />, title: "Traçabilité totale", desc: "Suivez vos transactions et l'état de vos demandes en temps réel depuis votre tableau de bord." }
                        ].map((item, i) => (
                            <motion.div
                                key={i} initial="hidden" whileInView="visible" viewport={{ once: true }} variants={{ ...fadeUp, transition: { delay: i * 0.15 } }}
                                className="group bg-white/[0.02] backdrop-blur-sm p-10 rounded-3xl border border-white/5 hover:border-white/10 transition-all duration-500 text-center overflow-hidden"
                            >
                                <div className="absolute inset-0 bg-gradient-to-br from-blue-500/5 to-transparent opacity-0 group-hover:opacity-100 transition-opacity duration-500 pointer-events-none"></div>

                                <div className="relative z-10">
                                    <div className="w-16 h-16 mx-auto mb-8 bg-white/5 rounded-2xl flex items-center justify-center text-white/40 border border-white/5 group-hover:text-white/80 transition-colors">
                                        {item.icon}
                                    </div>
                                    <h3 className="text-xl font-bold text-white mb-4">{item.title}</h3>
                                    <p className="text-white/30 text-sm leading-relaxed">{item.desc}</p>
                                </div>
                            </motion.div>
                        ))}
                    </div>

                    {/* Boutons CTA sous les cartes */}
                    <motion.div initial="hidden" whileInView="visible" viewport={{ once: true }} variants={fadeUp} className="text-center mt-16 flex flex-col sm:flex-row gap-4 justify-center">
                        <button className="bg-white text-[#0A0A0F] px-8 py-4 rounded-full font-bold hover:bg-gray-100 transition-colors shadow-lg shadow-white/5">
                            Se connecter
                        </button>
                        <button className="text-white/60 border border-white/10 px-8 py-4 rounded-full font-bold hover:bg-white/5 transition-colors">
                            Créer un compte
                        </button>
                    </motion.div>
                </div>
            </section>

            {/* ═══════ FOOTER ═══════ */}
            <footer className="relative z-10 border-t border-white/5 py-8 px-6">
                <div className="max-w-6xl mx-auto flex flex-col md:flex-row justify-between items-center gap-4">
                    <div className="text-lg font-bold tracking-widest text-white/20">KOLA</div>
                    <p className="text-xs text-white/10">© 2026 Kola Financial Technologies. Tous droits réservés.</p>
                </div>
            </footer>

        </div>
    );
}