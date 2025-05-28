import React from 'react';
import { Layout, ConfigProvider } from 'antd';
import zhCN from 'antd/locale/zh_CN';
import 'antd/dist/reset.css';
import './App.css';
import AppHeader from './components/AppHeader';
import MainContent from './components/MainContent';

const { Content } = Layout;

const App: React.FC = () => {
  return (
    <ConfigProvider locale={zhCN}>
      <Layout style={{ minHeight: '100vh', background: '#f5f7fa' }}>
        <AppHeader />
        <Content style={{ padding: '0' }}>
          <MainContent />
        </Content>
      </Layout>
    </ConfigProvider>
  );
};

export default App;
