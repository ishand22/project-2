/** @vitest-environment jsdom */
import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import { DataTable, statusClass } from './App';

describe('order status display', () => {
  it('maps completed orders to their success badge', () => {
    expect(statusClass('COMPLETED')).toBe('badge completed');
  });
  it('renders failed order status and retry action', () => {
    render(<DataTable rows={[{ id: 2, orderNumber: 'ORD-2', status: 'FAILED' }]} columns={['orderNumber','status']} onRetry={() => {}} />);
    expect(screen.getByText('FAILED')).toBeTruthy();
    expect(screen.getByRole('button', { name: 'Retry' })).toBeTruthy();
  });
});
