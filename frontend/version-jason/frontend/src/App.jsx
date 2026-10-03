import { useEffect, useState } from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './contextos/AuthContext';
import Protegida from './componentes/Protegida';
import BarraLateral from './componentes/BarraLateral';
import Encabezado from './componentes/Encabezado';
import PiePagina from './componentes/PiePagina';
import Login from './pantallas/Login';
import Home from './pantallas/Home';
import Equipos from './pantallas/Equipos';
import Movimientos from './pantallas/Movimientos';
import Catalogo from './pantallas/Catalogo';
import Laboratorios from './pantallas/Laboratorios';
import Asignaciones from './pantallas/Asignaciones';
import ModuloVisual from './pantallas/ModuloVisual';
import './App.css';

function Layout(){
  const [menu,setMenu]=useState(false);
  useEffect(() => {
    if (!menu) return;
    const cerrar = (event) => { if (event.key === 'Escape') setMenu(false); };
    window.addEventListener('keydown', cerrar);
    return () => window.removeEventListener('keydown', cerrar);
  }, [menu]);
  return <div className="app-layout">
    {menu && <button className="menu-fondo" aria-label="Cerrar menú" onClick={()=>setMenu(false)}/>}
    <BarraLateral abierta={menu} onClose={()=>setMenu(false)}/>
    <div className="contenido">
      <Encabezado onMenu={()=>setMenu(true)}/>
      <main><Routes>
        <Route path="/" element={<Home/>}/>
        <Route path="/equipos" element={<Equipos/>}/>
        <Route path="/categorias" element={<Catalogo/>}/>
        <Route path="/catalogo" element={<Catalogo/>}/>
        <Route path="/laboratorios" element={<Laboratorios/>}/>
        <Route path="/usuarios" element={<Protegida roles={['ADMIN']}><Asignaciones/></Protegida>}/>
        <Route path="/asignaciones" element={<Protegida roles={['ADMIN']}><Asignaciones/></Protegida>}/>
        <Route path="/movimientos" element={<Movimientos/>}/>
        <Route path="/mantenimientos" element={<ModuloVisual titulo="Mantenimientos" subtitulo="Programación y seguimiento del mantenimiento de equipos."/>}/>
        <Route path="/reportes" element={<ModuloVisual titulo="Reportes" subtitulo="Indicadores y exportación de información del inventario."/>}/>
        <Route path="/configuracion" element={<Protegida roles={['ADMIN']}><ModuloVisual titulo="Configuración" subtitulo="Preferencias generales y seguridad del sistema."/></Protegida>}/>
        <Route path="*" element={<Navigate to="/" replace/>}/>
      </Routes></main>
      <PiePagina/>
    </div>
  </div>
}

export default function App(){return <BrowserRouter><AuthProvider><Routes><Route path="/login" element={<Login/>}/><Route path="*" element={<Protegida><Layout/></Protegida>}/></Routes></AuthProvider></BrowserRouter>}
