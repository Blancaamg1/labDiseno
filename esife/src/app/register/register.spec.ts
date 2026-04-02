import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable } from 'rxjs';
import { vi } from 'vitest';

import { Register } from './register';

describe('Register', () => {
  let component: Register;
  let fixture: ComponentFixture<Register>;
  let httpSpy: { post: ReturnType<typeof vi.fn> };
  let routerSpy: { navigate: ReturnType<typeof vi.fn> };

  beforeEach(async () => {
    httpSpy = { post: vi.fn() };
    routerSpy = { navigate: vi.fn() };

    await TestBed.configureTestingModule({
      imports: [Register],
      providers: [
        { provide: HttpClient, useValue: httpSpy },
        { provide: Router, useValue: routerSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(Register);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('debe liberar loading y mostrar error en fallo 400', async () => {
    httpSpy.post.mockReturnValue(
      new Observable((subscriber) => {
        subscriber.error({ status: 400, error: 'Invalid credentials' });
      })
    );

    component.username = 'Mario';
    component.email = 'mario@gmail.com';
    component.validatePassword('Abcdefg1');
    component.pwd2 = 'Abcdefg1';

    await component.registrarse();
    fixture.detectChanges();

    expect(component.loading).toBeFalsy();
    expect(component.error).toContain('No se pudo crear la cuenta');
    expect(component.success).toBe('');
  });

  it('debe poner loading true durante petición y false al finalizar', async () => {
    let completeRequest: (() => void) | undefined;

    httpSpy.post.mockReturnValue(
      new Observable((subscriber) => {
        completeRequest = () => {
          subscriber.next('ok');
          subscriber.complete();
        };
      })
    );

    component.username = 'Mario';
    component.email = 'mario@gmail.com';
    component.validatePassword('Abcdefg1');
    component.pwd2 = 'Abcdefg1';

    const promise = component.registrarse();
    expect(component.loading).toBeTruthy();

    completeRequest?.();
    await promise;

    expect(component.loading).toBeFalsy();
    expect(component.success).toContain('ok');
  });
});
