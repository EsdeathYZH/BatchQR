#include "ufs.h"

UFS::USF(int N_): N(N_) {
	std::vector<int>(N,-1).swap(parents);
}

int UFS::find(int s) const {
	if(parents[s]<0) return s;
	else return find(parents[s]);
}

void UFS::get(std::vector<int>& rs) const {
	std::vector<int>().swap(rs);
	for(int i=0; i<N; ++i) if(parents[i]<0) rs.push_back(i);
}

void UFS::join(int s1, int s2) {
	int r1 = find(s1);
	int r2 = find(s2);

	if(r1==r2) return;
	if(parents[r2]<parents[r1]) parents[r1] = r2;
	else {
		if(parents[r1]==parents[r2]) --parents[r1];
		parents[r2] = r1;
	}
}