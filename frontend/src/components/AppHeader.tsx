import React from 'react';
import { Layout, Typography } from 'antd';

const { Header } = Layout;
const { Title } = Typography;

const AppHeader: React.FC = () => {
  return (
    <Header 
      style={{ 
        background: '#ffffff',
        boxShadow: '0 1px 4px rgba(0,0,0,0.08)',
        padding: '0 24px',
        height: '64px',
        display: 'flex',
        alignItems: 'center',
        borderBottom: '1px solid #f0f0f0'
      }}
    >
      <Title 
        level={4} 
        style={{ 
          margin: 0,
          color: '#000',
          fontWeight: '500'
        }}
      >
        TechDaily
      </Title>
    </Header>
  );
};

export default AppHeader; 