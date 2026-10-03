import { useEffect, useRef, useState } from 'react';
import { Navigate } from 'react-router-dom';
import { FlaskConical, LockKeyhole, UserRound, Eye, EyeOff, ArrowRight, ShieldCheck, Settings2, BarChart3, Microscope } from 'lucide-react';
import { useAuth } from '../contextos/AuthContext';
import { esCancelacion, mensajeError } from '../servicios/api';
import Boton from '../componentes/Boton';
import './Login.css';

export default function Login(){
 const {token,login,cancelarLogin,mensajeSesion}=useAuth();
 const [userName,setUserName]=useState(''); const [password,setPassword]=useState(''); const [mostrar,setMostrar]=useState(false);
 const [error,setError]=useState(''); const [cargando,setCargando]=useState(false);
 const enviando=useRef(false); const montado=useRef(true);
 useEffect(()=>{montado.current=true;return ()=>{montado.current=false;cancelarLogin();};},[cancelarLogin]);
 if(token)return <Navigate to="/" replace/>;
 async function submit(e){
   e.preventDefault();
   if(enviando.current)return;
   if(!userName.trim()){setError('Ingresa tu usuario.');return;}
   enviando.current=true;setError('');setCargando(true);
   try{await login(userName,password);if(montado.current){setPassword('');setMostrar(false);}}
   catch(err){if(montado.current&&!esCancelacion(err))setError(mensajeError(err,'No fue posible iniciar sesión. Intenta nuevamente.'));}
   finally{enviando.current=false;if(montado.current)setCargando(false);}
 }
 return <main className="login-pantalla">
   <section className="login-card">
     <div className="login-marca"><div className="login-logo"><FlaskConical size={34}/></div><div><strong>Inventario de</strong><span>Laboratorios</span></div></div>
     <div className="login-titulo"><h2>Iniciar sesión</h2><p>Accede al sistema de gestión de laboratorios</p></div>
     <form onSubmit={submit} aria-busy={cargando}>
       <label>Usuario<div className="campo-icono"><UserRound size={18}/><input type="text" value={userName} onChange={e=>setUserName(e.target.value)} required maxLength={50} autoComplete="username" placeholder="Ingresa tu usuario" disabled={cargando}/></div></label>
       <label>Contraseña<div className="campo-icono"><LockKeyhole size={18}/><input type={mostrar?'text':'password'} value={password} onChange={e=>setPassword(e.target.value)} required maxLength={72} autoComplete="current-password" placeholder="Ingresa tu contraseña"/><button type="button" className="ver-password" onClick={()=>setMostrar(v=>!v)} aria-label={mostrar?'Ocultar contraseña':'Mostrar contraseña'}>{mostrar?<EyeOff size={18}/>:<Eye size={18}/>}</button></div></label>
       {(error||mensajeSesion)&&<div className="mensaje-error" role="alert">{error||mensajeSesion}</div>}
       <Boton tipo="submit" disabled={cargando} className="btn-login">{cargando?'Validando...':'Ingresar'} <ArrowRight size={18}/></Boton>
     </form>
   </section>
   <section className="login-presentacion">
     <div className="login-ilustracion" aria-hidden="true">
       <div className="monitor"><div className="monitor-top"><span></span><span></span><span></span></div><div className="monitor-body"><div className="monitor-side"><i></i><i></i><i></i><i></i></div><div className="monitor-content"><b></b><b></b><b></b><div className="monitor-bars"><span></span><span></span><span></span></div></div></div></div>
       <div className="lab-flask"><FlaskConical size={62}/></div><div className="microscope"><Microscope size={74}/></div><div className="bottle bottle-a"></div><div className="bottle bottle-b"></div>
     </div>
     <div className="login-presentacion-text"><h1>Control y trazabilidad<br/>para tus laboratorios</h1><p>Gestiona equipos y laboratorios. Consulta su estado, sus responsables y el historial de traslados en un solo lugar.</p></div>
     <div className="login-beneficios"><div><ShieldCheck size={25}/><span>Información<br/>segura</span></div><div><Settings2 size={25}/><span>Procesos<br/>eficientes</span></div><div><BarChart3 size={25}/><span>Laboratorios<br/>mejor gestionados</span></div></div>
   </section>
 </main>;
}
