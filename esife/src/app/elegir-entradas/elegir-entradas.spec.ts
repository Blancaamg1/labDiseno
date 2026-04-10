import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ElegirEntradas } from './elegir-entradas';

describe('ElegirEntradas', () => {
  let component: ElegirEntradas;
  let fixture: ComponentFixture<ElegirEntradas>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ElegirEntradas],
    }).compileComponents();

    fixture = TestBed.createComponent(ElegirEntradas);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
