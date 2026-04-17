import { braucheZweitfreigabe } from './vier-augen';

describe('braucheZweitfreigabe', () => {
  it('meldet ja bei Downgrade auf NOT_APPLICABLE', () => {
    expect(braucheZweitfreigabe('NOT_APPLICABLE', 'CRITICAL')).toBeTrue();
  });

  it('meldet ja bei Downgrade auf INFORMATIONAL', () => {
    expect(braucheZweitfreigabe('INFORMATIONAL', 'HIGH')).toBeTrue();
  });

  it('meldet nein bei Downgrade von HIGH auf MEDIUM', () => {
    expect(braucheZweitfreigabe('MEDIUM', 'HIGH')).toBeFalse();
  });

  it('meldet nein bei Hochstufung', () => {
    expect(braucheZweitfreigabe('CRITICAL', 'HIGH')).toBeFalse();
  });

  it('meldet ja ohne Original, wenn Zielwert ein Vier-Augen-Zielwert ist', () => {
    expect(braucheZweitfreigabe('NOT_APPLICABLE')).toBeTrue();
    expect(braucheZweitfreigabe('INFORMATIONAL', null)).toBeTrue();
  });
});
