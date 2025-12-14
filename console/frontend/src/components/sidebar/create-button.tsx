import { ReactElement, useState } from 'react';
import { useTranslation } from 'react-i18next';
import MakeCreateModal from '../make-creation';
import { PlusOutlined } from '@ant-design/icons';

interface CreateButtonProps {
  isLogin?: boolean;
  onClick?: (() => void) | undefined;
  onAnalytics?: (() => void) | undefined;
  onNotLogin?: (() => void) | undefined;
}

const CreateButton = ({
  isLogin = false,
  onClick,
  onAnalytics,
  onNotLogin,
}: CreateButtonProps): ReactElement => {
  const { t } = useTranslation();
  const [makeModalVisible, setMakeModalVisible] = useState(false);

  const handleClick = (): void => {
    setMakeModalVisible(true);
    // 统计事件
    if (onAnalytics) {
      onAnalytics();
    }

    // 检查登录状态
    if (!isLogin) {
      if (onNotLogin) {
        onNotLogin();
      }
      return;
    }

    // 处理 bd_vid 参数
    const bdVid = sessionStorage.getItem('bd_vid');
    const currentUrl = new URL(window.location.href);

    if (bdVid) {
      currentUrl.searchParams.set('bd_vid', bdVid);
      window.history.pushState({}, '', currentUrl.toString());
    }

    // 执行点击回调
    if (onClick) {
      onClick();
    }
  };

  return (
    <div className="w-full mt-4">
      <div
        className={`
          h-9 rounded-[10px] bg-[#DCE6FF] flex items-center justify-between cursor-pointer
          transition-opacity duration-200 hover:bg-[#C6D4FF] px-[20px]
          w-full
        `}
        onClick={handleClick}
      >
        {
          <span className="flex-1 ml-2 font-bold font-sans text-sm leading-6 tracking-[-0.2px] text-[#265dfe]">
            {t('sidebar.create')}
          </span>
        }
        <div className="w-[2px] h-[14px] bg-[#265dfe50] rounded-[4px]"></div>
        <PlusOutlined className="ml-[20px] text-[#265dfe]" />
      </div>
      {makeModalVisible && (
        <MakeCreateModal
          visible={makeModalVisible}
          onCancel={() => {
            setMakeModalVisible(false);
          }}
        />
      )}
    </div>
  );
};

export default CreateButton;
