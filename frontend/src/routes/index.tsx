import { createBrowserRouter, Navigate } from 'react-router-dom'
import { AppLayout } from '@/layouts/AppLayout'
import { AuthLayout } from '@/layouts/AuthLayout'
import { DashboardPlaceholder } from '@/features/dashboard/DashboardPlaceholder'
import { WorkspacesPlaceholder } from '@/features/workspaces/WorkspacesPlaceholder'
import { ProductsPlaceholder } from '@/features/products/ProductsPlaceholder'
import { LicensingPlaceholder } from '@/features/licensing/LicensingPlaceholder'
import { CustomersPlaceholder } from '@/features/customers/CustomersPlaceholder'
import { SettingsPlaceholder } from '@/features/settings/SettingsPlaceholder'
import { AuthPlaceholder } from '@/features/auth/AuthPlaceholder'

export const router = createBrowserRouter([
  {
    path: '/',
    element: <AppLayout />,
    children: [
      { index: true, element: <DashboardPlaceholder /> },
      { path: 'workspaces', element: <WorkspacesPlaceholder /> },
      { path: 'products', element: <ProductsPlaceholder /> },
      { path: 'licenses', element: <LicensingPlaceholder /> },
      { path: 'customers', element: <CustomersPlaceholder /> },
      { path: 'settings', element: <SettingsPlaceholder /> },
    ],
  },
  {
    path: '/auth',
    element: <AuthLayout />,
    children: [{ index: true, element: <AuthPlaceholder /> }],
  },
  {
    path: '*',
    element: <Navigate to="/" replace />,
  },
])
