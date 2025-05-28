import React, { useState } from 'react';
import { Layout, Menu } from 'antd';
import {
  DashboardOutlined,
  FileTextOutlined,
  TrophyOutlined,
  BarChartOutlined,
  SettingOutlined,
  SearchOutlined,
  CalendarOutlined,
  RobotOutlined
} from '@ant-design/icons';

const { Sider } = Layout;

interface AppSidebarProps {
  onMenuSelect?: (key: string) => void;
}

const AppSidebar: React.FC<AppSidebarProps> = ({ onMenuSelect }) => {
  const [collapsed, setCollapsed] = useState(false);
  const [selectedKey, setSelectedKey] = useState('dashboard');

  const menuItems = [
    {
      key: 'dashboard',
      icon: <DashboardOutlined />,
      label: '仪表盘',
    },
    {
      key: 'articles',
      icon: <FileTextOutlined />,
      label: '技术文章',
    },
    {
      key: 'popular',
      icon: <TrophyOutlined />,
      label: '热门文章',
    },
    {
      key: 'daily-report',
      icon: <CalendarOutlined />,
      label: '每日日报',
    },
    {
      key: 'ai-summary',
      icon: <RobotOutlined />,
      label: 'AI总结',
    },
    {
      key: 'search',
      icon: <SearchOutlined />,
      label: '文章搜索',
    },
    {
      key: 'analytics',
      icon: <BarChartOutlined />,
      label: '数据分析',
    },
    {
      key: 'settings',
      icon: <SettingOutlined />,
      label: '系统设置',
    },
  ];

  const handleMenuClick = ({ key }: { key: string }) => {
    setSelectedKey(key);
    onMenuSelect?.(key);
  };

  return (
    <Sider 
      collapsible 
      collapsed={collapsed} 
      onCollapse={setCollapsed}
      style={{
        background: '#fff',
        boxShadow: '2px 0 8px rgba(0,0,0,0.1)'
      }}
    >
      <Menu
        mode="inline"
        selectedKeys={[selectedKey]}
        style={{ height: '100%', borderRight: 0 }}
        items={menuItems}
        onClick={handleMenuClick}
      />
    </Sider>
  );
};

export default AppSidebar; 