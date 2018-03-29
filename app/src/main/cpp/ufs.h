#include <vector>

class InValidIndex{};
class UFS
{
public:
	UFS(int N_);
	int find(int s) const;
	void get(std::vector<int> rs) const;
	void join(int s1, int s2);

private:
	std::vector<int> parents;
	int N;
};
