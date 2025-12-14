import React, { useState, useEffect, useMemo } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { CloseOutlined } from '@ant-design/icons';
import RetractableInput from '@/components/ui/global/retract-table-input';
import { Select } from 'antd';
import { ModelInfo } from '@/types/model';

interface ModelManagementHeaderProps {
  activeTab: string;
  shelfOffModel: ModelInfo[];
  refreshModels?: () => void;
  searchInput: string;
  setSearchInput?: (value: string) => void;
  filterType?: number;
  setFilterType?: (val: number) => void;
  setShowShelfOnly: (val: boolean) => void;
}

const ModelManagementHeader: React.FC<ModelManagementHeaderProps> = ({
  activeTab: initialActiveTab,
  shelfOffModel,
  refreshModels,
  searchInput,
  setSearchInput,
  filterType = 0,
  setFilterType,
  setShowShelfOnly,
}) => {
  const { t } = useTranslation();
  const [activeTab, setActiveTab] = useState(initialActiveTab);

  useEffect(() => {
    setActiveTab(initialActiveTab);
  }, [initialActiveTab]);

  /* 控制提示框是否已手动关闭 */
  const [closed, setClosed] = useState(false);

  /* 即将下架模型数量 */
  const offCount = useMemo(() => shelfOffModel.length, [shelfOffModel]);

  const getRobotsDebounce = (e: React.ChangeEvent<HTMLInputElement>): void => {
    const value = e.target.value;
    if (setSearchInput) {
      setSearchInput(value);
    }
  };

  const handleTypeChange = (val: number): void => {
    setFilterType?.(val);
  };

  const handleClose = (): void => {
    setClosed(true);
    setShowShelfOnly(false);
    if (activeTab === 'officialModel') {
      sessionStorage.removeItem('officialModelQueckFilter');
    }

    if (activeTab === 'personalModel') {
      sessionStorage.removeItem('personalModelQueckFilter');
    }
  };

  return (
    <div className="w-full max-w-[1425px] mx-auto">
      <div className="w-full relative z-10 flex flex-col justify-between rounded-2xl">
        <div className="flex items-center mb-4">
          <div className="page-title">{t('model.modelManagement')}</div>
          {/* 右侧控件 */}
          <div className="ml-auto flex items-center gap-4">
            {activeTab === 'personalModel' && (
              <Select
                placeholder={t('model.pleaseSelect')}
                value={filterType}
                style={{ width: 120 }}
                options={[
                  { label: t('model.all'), value: 0 },
                  { label: t('model.thirdPartyModel'), value: 1 },
                  { label: t('model.localModel'), value: 2 },
                ]}
                onChange={handleTypeChange}
              />
            )}
            <RetractableInput
              value={searchInput}
              restrictFirstChar={true}
              onChange={getRobotsDebounce}
            />
          </div>
        </div>
      </div>
    </div>
  );
};

export default ModelManagementHeader;
