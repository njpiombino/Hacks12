import { fromInputs, monthGrid, sameDay, toDateInput, toTimeInput } from './dates';

describe('dates', () => {
  it('builds a 6-week grid starting on the Sunday before the 1st', () => {
    const grid = monthGrid(new Date(2026, 8, 15)); // September 2026 starts on a Tuesday
    expect(grid.length).toBe(42);
    expect(grid[0].getDay()).toBe(0);
    expect(sameDay(grid[2], new Date(2026, 8, 1))).toBe(true);
  });

  it('round-trips date and time inputs in local time', () => {
    const d = fromInputs('2026-03-07', '09:05');
    expect(toDateInput(d)).toBe('2026-03-07');
    expect(toTimeInput(d)).toBe('09:05');
  });
});
