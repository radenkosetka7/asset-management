import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { forkJoin } from 'rxjs';
import { AssetService } from '../../../services/asset.service';
import { AssetStatus, CategoryResponse } from '../../../models/asset.model';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.css'
})
export class DashboardComponent implements OnInit {
  totalAssets    = signal(0);
  totalCategories = signal(0);
  statusCounts   = signal<Record<AssetStatus, number>>({ ACTIVE: 0, IN_USE: 0, DAMAGED: 0, RETIRED: 0 });
  loading = signal(true);
  error   = signal<string | null>(null);

  constructor(private assetService: AssetService) {}

  ngOnInit(): void {
    forkJoin({
      all:      this.assetService.getAll(0, 1, 'id,asc'),
      active:   this.assetService.filter({ assetStatus: 'ACTIVE'  }, 0, 1),
      inUse:    this.assetService.filter({ assetStatus: 'IN_USE'  }, 0, 1),
      damaged:  this.assetService.filter({ assetStatus: 'DAMAGED' }, 0, 1),
      retired:  this.assetService.filter({ assetStatus: 'RETIRED' }, 0, 1),
      categories: this.assetService.getAllCategories(),
    }).subscribe({
      next: ({ all, active, inUse, damaged, retired, categories }) => {
        this.totalAssets.set(all.page.totalElements);
        this.totalCategories.set((categories as CategoryResponse[]).length);
        this.statusCounts.set({
          ACTIVE:  active.page.totalElements,
          IN_USE:  inUse.page.totalElements,
          DAMAGED: damaged.page.totalElements,
          RETIRED: retired.page.totalElements,
        });
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Failed to load dashboard data.');
        this.loading.set(false);
      }
    });
  }

  get quickStats() {
    const s = this.statusCounts();
    return [
      { label: 'Total Assets', value: this.totalAssets(),     color: 'blue'   },
      { label: 'Active',       value: s['ACTIVE'],             color: 'green'  },
      { label: 'In Use',       value: s['IN_USE'],             color: 'indigo' },
      { label: 'Damaged',      value: s['DAMAGED'],            color: 'red'    },
      { label: 'Retired',      value: s['RETIRED'],            color: 'gray'   },
      { label: 'Categories',   value: this.totalCategories(),  color: 'purple' },
    ];
  }
}



