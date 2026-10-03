import { BarChart3, Settings, Wrench } from 'lucide-react';

const iconos={Mantenimientos:Wrench,Reportes:BarChart3,Configuración:Settings};
export default function ModuloVisual({titulo,subtitulo}){
 const Icon=iconos[titulo]||Settings;
 return <div className="pagina"><div className="pagina-cabecera"><div><span className="eyebrow">Próximamente</span><h1>{titulo}</h1><p>{subtitulo}</p></div></div><section className="panel modulo-placeholder"><Icon size={42}/><h2>Fuera del alcance actual</h2><p>Este módulo forma parte del diseño propuesto. Su pantalla funcional y sus operaciones todavía no están implementadas.</p></section></div>
}
