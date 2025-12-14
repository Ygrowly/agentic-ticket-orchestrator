import { FC } from 'react';
import { ErrorBoundary } from 'react-error-boundary';
import { Outlet } from 'react-router-dom';
import CrashErrorComponent from '@/components/crash-error-component';
import Sidebar from '@/components/sidebar';

interface BasicLayoutProps {
  showHeader?: boolean;
}

const BasicLayout: FC<BasicLayoutProps> = ({ showHeader }) => {
  return (
    <ErrorBoundary
      onReset={() => {
        window.location.href = '/';
      }}
      FallbackComponent={CrashErrorComponent}
    >
      <div className="flex h-full w-full overflow-hidden global-background">
        <Sidebar />

        <div className="flex-1 flex flex-col overflow-hidden">
          <Outlet />
        </div>
      </div>
    </ErrorBoundary>
  );
};

export default BasicLayout;
