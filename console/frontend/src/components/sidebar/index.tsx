import { ReactElement, useState, useEffect } from 'react';
import collapseGrayIcon from '@/assets/imgs/sidebar/collapseGray.svg';
import SidebarLogo from './sidebar-logo';
import CreateButton from './create-button';
import BottomLogin from './bottom-login';
import PersonalCenter from './personal-center';
import MenuList from './menu-list';
import useUserStore from '@/store/user-store';
import { postChatList } from '@/services/chat';
import { PostChatItem } from '@/types/chat';
import eventBus from '@/utils/event-bus';

interface User {
  nickname?: string;
  login?: string;
  avatar?: string;
  uid?: string;
}

interface SidebarProps {
  className?: string;

  // Logo props
  languageCode?: string;

  // Create button props
  isLogin?: boolean;
  onCreateClick?: () => void;
  onCreateAnalytics?: () => void;
  onNotLogin?: () => void;

  // Bottom login props
  user?: User;
  OrderTypeComponent?: ReactElement;
}

const Sidebar = ({
  className = '',

  languageCode = 'zh',

  // Create button props
  onCreateClick,
  onCreateAnalytics,
  onNotLogin,

  // Bottom login props
  user,
  OrderTypeComponent,
}: SidebarProps): ReactElement => {
  const [isPersonCenterOpen, setIsPersonCenterOpen] = useState(false);

  // Shared chat data state
  const [mixedChatList, setMixedChatList] = useState<PostChatItem[]>([]);

  const getIsLogin = useUserStore.getState().getIsLogin;

  // Fetch chat list
  const getChatList = async () => {
    try {
      const res = await postChatList();
      setMixedChatList(res);
    } catch (error) {
      console.log(error);
    }
  };

  // Effect to fetch data on mount and setup event listeners
  useEffect(() => {
    getChatList();

    // Setup event bus listeners for data changes
    eventBus.on('chatListChange', getChatList);

    return () => {
      eventBus.off('chatListChange', getChatList);
    };
  }, []);

  return (
    <div
      className={`relative bg-transparent flex flex-col flex-shrink-0 p-4 h-full w-[232px]  ${className}`}
    >
      {/* Main Content */}
      <div className="flex flex-col h-full">
        {/* Logo Section */}
        <SidebarLogo />

        {/* Create Button */}
        <CreateButton
          isLogin={getIsLogin()}
          onClick={onCreateClick}
          onAnalytics={onCreateAnalytics}
          onNotLogin={onNotLogin}
        />

        <MenuList
          mixedChatList={mixedChatList}
          onRefreshData={() => {
            getChatList();
          }}
        />

        {/* Bottom Login */}
        <BottomLogin
          user={user}
          OrderTypeComponent={OrderTypeComponent}
          isPersonCenterOpen={isPersonCenterOpen}
          setIsPersonCenterOpen={setIsPersonCenterOpen}
        />

        <PersonalCenter
          open={isPersonCenterOpen}
          onCancel={() => {
            setIsPersonCenterOpen(false);
          }}
          mixedChatList={mixedChatList}
          onRefreshData={() => {
            getChatList();
          }}
          onRefreshRecentData={getChatList}
        />
      </div>
    </div>
  );
};

export default Sidebar;
