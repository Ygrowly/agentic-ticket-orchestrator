import React from 'react';
import ModelManagementHeader from '../components/model-management-header';
import ModelCardList from '../components/model-card-list';
import ModelModalComponents from '../components/model-modal-components';
import { ModelProvider, useModelContext } from '../context/model-context';
import { useModelInitializer } from '../hooks/use-model-initializer';
import { useModelOperations } from '../hooks/use-model-operations';
import { useModelFilters } from '../hooks/use-model-filters';

// 个人模型页面内容组件
const PersonalModelContent: React.FC = () => {
  const { state } = useModelContext();

  // 使用hooks
  useModelInitializer(2); // 2表示个人模型
  const operations = useModelOperations(2);
  const filters = useModelFilters();

  return (
    <div className="w-full h-screen flex flex-col">
      <div className="pr-19">
        <div className="flex-none pt-8 pb-2 sm:pb-2 lg:pb-2">
          <ModelManagementHeader
            activeTab="personalModel"
            shelfOffModel={state.shelfOffModels}
            searchInput={state.searchInput}
            setSearchInput={filters.handleSearchInputChange}
            refreshModels={operations.handleQuickFilter}
            filterType={state.filterType}
            setFilterType={filters.handleFilterTypeChange}
            setShowShelfOnly={operations.handleCloseQuickFilter}
          />
        </div>
      </div>

      <div className="flex-1 overflow-hidden">
        <div className="mx-auto h-full w-full flex flex-col lg:flex-row gap-6 lg:gap-8">
          {/* 右侧卡片 */}
          <main className="mx-auto col-span-4 rounded-lg overflow-y-auto [&::-webkit-scrollbar-thumb]:rounded-full w-full max-w-[1425px]">
            <ModelCardList
              models={filters.filteredModels}
              showCreate
              keyword={state.searchInput}
              filterType={state.filterType}
              setModels={operations.setModels}
              refreshModels={operations.refreshModels}
              showShelfOnly={state.showShelfOnly}
            />
          </main>
        </div>
      </div>

      {/* 模态框 */}
      <ModelModalComponents modelType={2} />
    </div>
  );
};

// 个人模型页面主组件（带Provider）
function PersonalModel(): React.JSX.Element {
  return (
    <ModelProvider>
      <PersonalModelContent />
    </ModelProvider>
  );
}

export default PersonalModel;
