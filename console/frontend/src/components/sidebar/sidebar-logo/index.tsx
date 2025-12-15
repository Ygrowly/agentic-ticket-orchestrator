import { ReactElement } from 'react';
import { useNavigate } from 'react-router-dom';
import agentLog from '@/assets/imgs/sidebar/logo.png';

const SidebarLogo = (): ReactElement => {
  const navigate = useNavigate();

  const handleLogoClick = (): void => {
    navigate('/');
  };

  return (
    <div className="flex items-center justify-center gap-1">
      <img
        src={agentLog}
        className="w-[140px] cursor-pointer"
        alt="PaiFlow"
        style={{ height: 'auto' }}
        onClick={handleLogoClick}
      />
    </div>
  );
};

export default SidebarLogo;